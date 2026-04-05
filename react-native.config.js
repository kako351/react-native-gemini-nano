module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.gemininano.GeminiNanoPackage;',
        packageInstance: 'new GeminiNanoPackage()',
      },
    },
  },
};
