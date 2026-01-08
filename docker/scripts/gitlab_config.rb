# This script can be used to automate the initial configuration of CATMA's backend GitLab server
# 
# To see the available settings, check https://docs.gitlab.com/api/settings/#available-settings and/or run the following in a rails console
# ([sudo] gitlab-rails console):
# 1. IRB.conf[:USE_PAGER] = false # ref: https://stackoverflow.com/questions/78630707/how-to-disable-pager-in-rails-console-so-entire-result-prints-to-console
# 2. ApplicationSetting.column_names
#
# To check a field's validators:
# ApplicationSetting.current.class.validators_on(:<field_name>) - eg: ApplicationSetting.current.class.validators_on(:service_ping_settings)
# Check some of the other links below for where you might find validation JSON schemas.
# If you get a validation error like "... is not included in the list" and validators_on returns nothing, this probably comes from GitLab’s custom
# allowed_values logic in the model, not from Rails validators. In that case there is likely a constant defined in ApplicationSetting, eg:
# ApplicationSetting::VALID_RUNNER_REGISTRAR_TYPES
# 
# To look at created PATs: p User.find_by_username('root').personal_access_tokens
# ref: https://stackoverflow.com/questions/354547/how-do-i-dump-an-objects-fields-to-the-console

if ARGV.length < 1
  abort "Usage: gitlab-rails runner gitlab_config.rb <admin_token_string>"
end
admin_token_string = ARGV[0]
pat_prefix = 'catma-glpat-' # TODO: parameterize

ApplicationSetting.current.update!(
  # these are absolutely necessary for CATMA to work:
  password_authentication_enabled_for_web: true, # apparently, this also enables the (deprecated) resource owner password credentials (ROPC) flow
  auto_devops_enabled: false,
  default_branch_name: 'master',
  # default_branch_protection: Gitlab::Access::PROTECTION_DEV_CAN_PUSH, # deprecated
  # refs: https://gitlab.com/gitlab-org/gitlab/-/blob/v18.5.5-ee/app/models/application_setting.rb?ref_type=tags
  #       https://gitlab.com/gitlab-org/gitlab/-/blob/v18.5.5-ee/app/validators/json_schemas/default_branch_protection_defaults.json?ref_type=tags
  default_branch_protection_defaults: {
    "allowed_to_push" => [{"access_level" => Gitlab::Access::DEVELOPER}],
    "allow_force_push" => false,
    "allowed_to_merge" => [{"access_level" => Gitlab::Access::MAINTAINER}],
    "developer_can_initial_push" => false
  },

  # these are nice-to-have:
  restricted_visibility_levels: [Gitlab::VisibilityLevel::INTERNAL, Gitlab::VisibilityLevel::PUBLIC],
  gravatar_enabled: false,
  personal_access_token_prefix: pat_prefix,
  require_personal_access_token_expiry: false,
  user_oauth_applications: false,
  user_show_add_ssh_key_message: false,
  diff_max_patch_bytes: 512000,
  signup_enabled: false,
  require_admin_approval_after_user_signup: false,
  email_confirmation_setting: :hard,
  password_authentication_enabled_for_git: true,
  silent_mode_enabled: true,
  diagramsnet_enabled: false,
  hide_third_party_offers: true,
  # ref: https://gitlab.com/gitlab-org/gitlab/-/blob/v18.5.5-ee/app/validators/json_schemas/application_setting_search.json?ref_type=tags
  search: {
    "global_search_issues_enabled" => true,
    "global_search_merge_requests_enabled" => true,
    "global_search_snippet_titles_enabled" => false,
    "global_search_users_enabled" => true,
    "anonymous_searches_allowed" => false
  },
  mirror_available: false,
  shared_runners_enabled: false,
  suggest_pipeline_enabled: false,
  show_migrate_from_jenkins_banner: false,
  allow_runner_registration_token: false,
  # refs: https://gitlab.com/gitlab-org/gitlab/-/blob/v18.5.5-ee/app/models/application_setting.rb?ref_type=tags
  #       https://gitlab.com/gitlab-org/gitlab/-/blob/v18.5.5-ee/app/models/application_setting_implementation.rb?ref_type=tags (VALID_RUNNER_REGISTRAR_TYPES)
  valid_runner_registrars: [],
  prometheus_metrics_enabled: false,
  # the following 3 usage/service ping settings may move into 'service_ping_settings' in later versions
  usage_ping_enabled: false,
  usage_ping_generation_enabled: false,
  include_optional_metrics_in_service_ping: false,
  usage_ping_features_enabled: false,
  # ref: https://gitlab.com/gitlab-org/gitlab/-/blob/v18.5.5-ee/app/validators/json_schemas/application_setting_service_ping_settings.json?ref_type=tags
  service_ping_settings: {
    "gitlab_environment_toolkit_instance" => false,
    "gitlab_product_usage_data_enabled" => false
  },
  snowplow_enabled: false,
  whats_new_variant: "disabled",
  help_page_hide_commercial_content: true,
  help_page_support_url: "https://catma.de/about/contact/",
  first_day_of_week: 1 # Monday
)

# appearance settings TODO: parameterize app URL
appearance = Appearance.first_or_create!
appearance.update!(
  favicon: File.open("/var/opt/gitlab/gitlab-rails/uploads/catma-gitlab-combo-favicon.ico"),
  header_logo: File.open("/var/opt/gitlab/gitlab-rails/uploads/catma-gitlab-combo-logo-blue-on-white-pill-50a.svg"),
  title: "CATMA GitLab",
  description: <<EOF
This is CATMA's self-managed GitLab backend.

**NB:** Before you can sign in here, you need to have created an account directly in the [CATMA application](https://app.catma.de/)!

Confused? Perhaps you are looking for:\
→ The [CATMA application](https://app.catma.de/)\
→ More information on CATMA's [Git Access](https://catma.de/documentation/git-access/)\
→ The CATMA [website](https://catma.de/), including tutorials, FAQs and other documentation
EOF
)

# create the admin PAT, ref: https://docs.gitlab.com/user/profile/personal_access_tokens/#create-a-personal-access-token-programmatically
admin_user = User.find_by_username('root')
admin_pat = admin_user.personal_access_tokens.create(scopes: ['api', 'sudo', 'admin_mode'], name: 'CATMA')
admin_pat.set_token("#{pat_prefix}#{admin_token_string}")
admin_pat.save!

# create the default user, ref: https://docs.gitlab.com/user/profile/account/create_accounts/?tab=17.7+and+later#create-a-user-through-the-rails-console
user = Users::CreateService.new( # TODO: parameterize user details
  admin_user,
  username: 'standalone',
  email: 'standalone@catma.de',
  name: 'standalone',
  password: 'St4nd@lone',
  password_confirmation: 'St4nd@lone',
  organization_id: Organizations::Organization.first.id,
  skip_confirmation: true
).execute
if user.error?
  abort "Failed to create user! Message: #{user.message}; Cause: #{user.cause}"
end
