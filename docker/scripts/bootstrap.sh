#!/bin/bash

red='\033[31m'
green='\033[32m'
yellow='\033[33m'
blue='\033[34m'
gray='\033[37m'

bold='\033[1m'
italic='\033[3m'
ul='\033[4m'
clear='\033[0m'

color_red(){
  echo -ne $red$1$clear
}
color_green(){
  echo -ne $green$1$clear
}
color_yellow(){
  echo -ne $yellow$1$clear
}
color_blue(){
  echo -ne $blue$1$clear
}
color_gray(){
  echo -ne $gray$1$clear
}

print_logo(){
echo -ne "
$bold
$blue   ______       _     _________  ____    ____       _       
$blue .' ___  |     / \   |  _   _  ||_   \  /   _|     / \      
$blue/ .'   \_|    / _ \  |_/ | | \_|  |   \/   |      / _ \     
$blue| |          / ___ \     | |      | |\  /| |     / ___ \    
$blue\ \`.___.'\ _/ /   \ \_  _| |_    _| |_\/_| |_  _/ /   \ \_  
$blue \`.____ .'|____| |____||_____|  |_____||_____||____| |____| 

$clear$italic$gray\0https://catma.de
$clear$italic$gray\0CATMA $CATMA_VERSION (Standalone Version)
$clear"
}

shutdown_handler(){
  echo -e "\nShutting down, please wait ..."
  if [[ -e /opt/jetty_usr_home/run/jetty.pid ]]
  then
    service jetty stop &> /dev/null
  fi
  kill -s TERM $(ps -C init-container -o pid=)
  wait
  exit
}

stty -ctlecho
trap "shutdown_handler" INT TERM

print_logo

# 1. update config files based on environment variables
sed -i "s|^\(JAVA_OPTIONS=\"\).*$|\1$JETTY_JAVA_OPTIONS -server\"|" /etc/default/jetty
sed -i "s|^\(BASE_URL=\).*$|\1$CATMA_URL/|" /opt/jetty_web/catma_base/webapps/ROOT/catma.properties # note trailing slash, see CATMAPropertyKey
# the following requires '--add-host=gitlab.localhost=127.0.0.1' as part of the 'docker run' command because gitlab.localhost doesn't automatically resolve to
# 127.0.0.1 within the container
sed -i "s|^\(GITLAB_SERVER_URL=\).*$|\1$GITLAB_URL|" /opt/jetty_web/catma_base/webapps/ROOT/catma.properties
sed -i "s|^\(LOGOUT_URL=\).*$|\1$CATMA_URL|" /opt/jetty_web/catma_base/webapps/ROOT/catma.properties
sed -i "s|^\(RESET_PASSWORD_URL=\).*$|\1$GITLAB_URL/users/password/new|" /opt/jetty_web/catma_base/webapps/ROOT/catma.properties

# 2. if /etc/gitlab/gitlab.rb doesn't exist then we're starting for the first time - set a flag for later use, copy the template file and set some initial
#    config options
#    this is partially copied from GitLab's init-container script, ref: https://gitlab.com/gitlab-org/omnibus-gitlab/-/blob/master/docker/assets/init-container
if [[ ! -e /etc/gitlab/gitlab.rb ]]; then
  FIRST_START=true
  cp /opt/gitlab/etc/gitlab.rb.template /etc/gitlab/gitlab.rb
  chmod 0600 /etc/gitlab/gitlab.rb
  # our custom config follows
  # we always set these:
  cat << EOF >> /etc/gitlab/gitlab.rb


external_url = '${GITLAB_URL}'
nginx['listen_port'] = ${GITLAB_PORT}
gitlab_rails['gitlab_username_changing_enabled'] = false
prometheus_monitoring['enable'] = false

# following are additional recommendations as per: https://docs.gitlab.com/omnibus/settings/memory_constrained_envs/
puma['worker_processes'] = 0
sidekiq['concurrency'] = 10
EOF
fi

# 3. handle $LOW_MEM (optionally add settings for an even smaller memory footprint, at the expense of performance)
LOW_MEM_MARKER="## LOW MEMORY SETTINGS (don't add anything beyond this point, as it will be discarded if LOW_MEM=false!) ##"
if [[ $LOW_MEM = "true" || $LOW_MEM = "on" ]]
then
  echo -ne "\n$(color_gray 'INFO: Low memory mode enabled')"
  # add extra settings if they are not already there:
  if ! grep -Fxq "$LOW_MEM_MARKER" /etc/gitlab/gitlab.rb
  then
    cat << EOF >> /etc/gitlab/gitlab.rb

${LOW_MEM_MARKER}
gitlab_rails['env'] = {
  'MALLOC_CONF' => 'dirty_decay_ms:1000,muzzy_decay_ms:1000'
}
gitaly['configuration'] = {
    concurrency: [
      {
        'rpc' => "/gitaly.SmartHTTPService/PostReceivePack",
        'max_per_repo' => 3,
      }, {
        'rpc' => "/gitaly.SSHService/SSHUploadPack",
        'max_per_repo' => 3,
      },
    ],
    cgroups: {
        repositories: {
            count: 2,
        },
        mountpoint: '/sys/fs/cgroup',
        hierarchy_root: 'gitaly',
        memory_bytes: 500000,
        cpu_shares: 512,
    },
}
gitaly['env'] = {
  'MALLOC_CONF' => 'dirty_decay_ms:1000,muzzy_decay_ms:1000',
  'GITALY_COMMAND_SPAWN_MAX_PARALLEL' => '2'
}
EOF
  fi
else
  # remove extra settings if they are there:
  if grep -Fxq "$LOW_MEM_MARKER" /etc/gitlab/gitlab.rb
  then
    sed -i.bak -n "/$LOW_MEM_MARKER/q;p" /etc/gitlab/gitlab.rb
  fi
fi

# 4. start GitLab
echo -e "\n\nStarting GitLab ... (this will take a while, especially on the first run)"
echo -e "\n--- $(date)\n\n" >> /var/log/gitlab/gitlab_init.log
GITLAB_SKIP_TAIL_LOGS=true /assets/init-container &>> /var/log/gitlab/gitlab_init.log &
sleep 60
# testing showed the following start times with Docker constrained to 4 CPUs & 6GBs of RAM
# (this will of course vary depending on hardware and resources available to Docker)
# first start: ~1m 50s until nginx responded (~3m 50s with $LOW_MEM=true), another ~30s until 200 OK
# subsequent start: ~15s until nginx responded, another ~45s until 200 OK
#curl --retry 100 --retry-delay 5 --retry-connrefused -L -w "%{http_code}" -o /dev/null $GITLAB_URL

# wait up to another 4m 15s
curl --retry 8 --retry-connrefused -s -L -o /dev/null $GITLAB_URL

if [[ $? -ne 0 ]]
then
  cat << EOF
$(color_yellow "Aborting after waiting for more than 5 minutes!")
$(color_gray "GitLab may still start successfully, but we can't wait forever...\nTo troubleshoot, open a terminal in the still running container (e.g. \`docker
exec -it catma-standalone /bin/bash\`), inspect /var/log/gitlab/gitlab_init.log (e.g. \`tail --lines=50 /var/log/gitlab/gitlab_init.log\`) and run \`gitlab-ctl
status\`. Docker may need more CPU/RAM for GitLab to start within a reasonable timeframe. If you are sure that GitLab did eventually start successfully you can
choose to continue, otherwise you can send the details of the problem to support@catma.de")

EOF
  read -p "Try to continue anyway? (y/n [n]): " confirm && [[ $confirm = [yY] || $confirm = [yY][eE][sS] ]] || shutdown_handler
fi

# 5. declare variables used in subsequent steps
# although these gitlab_config.rb parameters have defaults, we declare and pass them in explicitly because they are re-used later 
PAT_PREFIX=catma-glpat-
DU_USERNAME=standalone
DU_PASSWORD=St4nd@lone

# 6. if this is the first start of the GitLab server, complete the initial GitLab setup (set default settings, create an admin PAT, create the default user)
#    also generate a CATMA API secret
if [[ $FIRST_START = "true" ]]
then
  echo -e "Configuring GitLab ..."
  echo -e "\n--- $(date)\n\n" >> /var/log/gitlab/gitlab_config.log
  GITLAB_UPLOADS_DIR=/var/opt/gitlab/gitlab-rails/uploads/
  cp /opt/assets/catma-gitlab-combo-favicon.ico /opt/assets/catma-gitlab-combo-logo-blue-on-white-pill-50a.svg $GITLAB_UPLOADS_DIR
  chown git:git ${GITLAB_UPLOADS_DIR}catma-gitlab-combo-favicon.ico ${GITLAB_UPLOADS_DIR}catma-gitlab-combo-logo-blue-on-white-pill-50a.svg
  ADMIN_TOKEN=$(pwgen -snc 20 1)
  # https://docs.gitlab.com/administration/operations/rails_console/#using-the-rails-runner
  gitlab-rails runner /opt/scripts/gitlab_config.rb --app_url $CATMA_URL --admin_token $ADMIN_TOKEN --pat_prefix $PAT_PREFIX --du_username $DU_USERNAME\
    --du_password $DU_PASSWORD &>> /var/log/gitlab/gitlab_config.log
  if [[ $? -ne 0 ]]
  then
    cat << EOF
$(color_red "Error! Couldn't complete the initial configuration of GitLab.")
$(color_gray "To troubleshoot, open a terminal in the still running container (e.g. \`docker exec -it catma-standalone /bin/bash\`) and inspect
/var/log/gitlab/gitlab_config.log (e.g. \`cat /var/log/gitlab/gitlab_config.log\`) - you can send the details of the problem to support@catma.de")

Hit <Enter> to exit $(color_red "(this will stop the container)")
EOF
    read
    shutdown_handler
  fi
  sed -i "s|^\(GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN=\).*$|\1${PAT_PREFIX}${ADMIN_TOKEN}|" /opt/jetty_web/catma_base/webapps/ROOT/catma.properties
  sed -i "s|^\(API_HMAC_SECRET=\).*$|\1$(pwgen -snc 32 1)|" /opt/jetty_web/catma_base/webapps/ROOT/catma.properties
fi

# 7. start Jetty
# we need to ensure correct ownership because these directories are specified as volumes - they will thus retain the origin ownership on the host unless we
# change it
chown -R jetty:jetty /opt/jetty_web/catma_base/logs /data
chown jetty:root /opt/jetty_temp

service jetty start \
  && cat << EOF

$(color_green "Success! You can now access CATMA at $CATMA_URL and log in with\nUsername: ${DU_USERNAME}\nPassword: ${DU_PASSWORD}")

$(color_gray "You can access the underlying GitLab backend at $GITLAB_URL and log in with the same credentials.\nFor admin access to GitLab,
use the username \"root\" and the password found in \$GITLAB_HOME/config/initial_root_password (you can also get the password by running e.g. \`docker exec -it
catma-standalone grep 'Password:' /etc/gitlab/initial_root_password\` in a separate terminal). Store the admin password somewhere safe, as you may need it again
later - the initial_root_password file is automatically deleted if the container is started again more than 24 hours after the first start.")

Hit <CTRL/CMD + C> to stop the container
EOF

# 8. wait for signals
wait
