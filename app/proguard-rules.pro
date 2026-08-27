# Project-specific R8 rules.
#
# The Android Gradle Plugin and dependencies contribute their own consumer rules.
# Keep this file for app-level rules that become necessary when adding reflection
# or other dynamically discovered types.

# PrettyTime resolves locale bundles by their fully qualified class names via
# ResourceBundle. R8 cannot see those reflective references, so preserve the
# bundle classes and their names for every supported app locale.
-keep class org.ocpsoft.prettytime.i18n.** { *; }
