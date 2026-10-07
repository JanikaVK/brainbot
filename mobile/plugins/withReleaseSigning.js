const { withAppBuildGradle } = require('@expo/config-plugins');

module.exports = function withReleaseSigning(config) {
  return withAppBuildGradle(config, (config) => {
    let contents = config.modResults.contents;
    if (!contents.includes("def keystorePath = System.getenv('ANDROID_KEYSTORE_PATH')")) {
      const signingBlock = /(signingConfigs\s*\{\s*debug\s*\{[^}]*\})/s;
      if (!signingBlock.test(contents)) {
        throw new Error('BrainBot could not locate Android signingConfigs while setting up release signing.');
      }
      contents = contents.replace(signingBlock, `$1
        release {
            def keystorePath = System.getenv('ANDROID_KEYSTORE_PATH')
            def storePasswordEnv = System.getenv('ANDROID_KEYSTORE_PASSWORD')
            def keyAliasEnv = System.getenv('ANDROID_KEY_ALIAS')
            def keyPasswordEnv = System.getenv('ANDROID_KEY_PASSWORD')
            if (keystorePath != null) {
                if ([storePasswordEnv, keyAliasEnv, keyPasswordEnv].any { it == null || it.isEmpty() }) {
                    throw new GradleException('Set all ANDROID_KEYSTORE_* and ANDROID_KEY_* environment variables for release signing.')
                }
                storeFile file(keystorePath)
                storePassword storePasswordEnv
                keyAlias keyAliasEnv
                keyPassword keyPasswordEnv
            }
        }`);
    }
    contents = contents.replace('signingConfig signingConfigs.debug', 'signingConfig signingConfigs.release');
    config.modResults.contents = contents;
    return config;
  });
};
