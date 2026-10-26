# Sets the global notification level to "disabled" for every non-admin user that doesn't already have it set that way.
#
# CATMA backs its comments feature with GitLab issues and notes, so GitLab's activity notifications (new note, new issue, ...) are noise for CATMA users.
# Nothing important is lost: GitLab sends security and membership emails - unknown sign-in, added to a project or group, project scheduled for deletion,
# token expiry, password changed, and so on - regardless of the notification level.
#
# Admins are left alone, because a CATMA instance's administrators are also the people running the GitLab server and may well want the activity.
#
# Everyone else's choice is overridden: a user who deliberately chose a level other than "disabled" has it reset too. A dry run reports the levels in use
# beforehand, so you can see how many users that affects.
#
# Usage:
#   [sudo] gitlab-rails runner /path/to/disable_user_notifications.rb
#   [sudo] DRY_RUN=1 gitlab-rails runner /path/to/disable_user_notifications.rb   # report what would change, change nothing (DRY_RUN goes after sudo,
#                                                                                 # which would otherwise drop it from the environment)

dry_run = ENV['DRY_RUN'].present?

DISABLED_LEVEL = NotificationSetting.levels[:disabled]
GLOBAL_SETTING = { source_id: nil, source_type: nil }.freeze

# Users that already have a global setting of "disabled" need no work. Everyone else does - including users with no global setting row at all, whose
# effective level is GitLab's default ("participating"). Calling User#global_notification_setting creates the row for those, so it must not run in a dry run.
users_already_disabled = NotificationSetting.where(GLOBAL_SETTING).where(level: DISABLED_LEVEL).select(:user_id)
candidates = User.where.not(id: users_already_disabled)

# report the starting position, which also shows the users with no global setting row (total users minus the sum of the levels below)
puts 'Global notification levels before:'
NotificationSetting.where(GLOBAL_SETTING).group(:level).count
  # depending on the Rails version this is keyed by the enum's label ("participating") or by its value (1), so normalise to the value, which also gives
  # us a sensible sort order
  .map { |level, count| [level.is_a?(Integer) ? level : NotificationSetting.levels[level.to_s], count] }
  .sort
  .each { |level, count| puts format('  %-14s %d', NotificationSetting.levels.key(level), count) }
puts format('  %-14s %d', '(no setting)', User.count - NotificationSetting.where(GLOBAL_SETTING).count)
puts

updated = 0
skipped_admins = 0
skipped_non_human = 0
failures = []

candidates.find_each(batch_size: 1_000) do |user|
  if user.admin?
    skipped_admins += 1
    next
  end

  # bots (support, alert, ...), the ghost user, and other GitLab-internal accounts aren't ours to change
  if (user.respond_to?(:bot?) && user.bot?) || (user.respond_to?(:internal?) && user.internal?)
    skipped_non_human += 1
    next
  end

  if dry_run
    updated += 1
    next
  end

  begin
    setting = user.global_notification_setting # creates the row, at GitLab's default level, if the user has none

    if setting.update(level: :disabled)
      updated += 1
    else
      failures << "#{user.username}: #{setting.errors.full_messages.join(', ')}"
    end
  rescue StandardError => e
    failures << "#{user.username}: #{e.class}: #{e.message}"
  end
end

puts dry_run ? 'DRY RUN - nothing was changed' : 'Done'
puts format('  users updated:           %d', updated)
puts format('  skipped (admins):        %d', skipped_admins)
puts format('  skipped (bots/internal): %d', skipped_non_human)
puts format('  failed:                  %d', failures.size)
failures.each { |failure| puts "    #{failure}" }
