// config solo para imprimir el V(G) de cada funcion: con limite 0 todas se reportan
module.exports = [
  {
    files: ['src/**/*.js'],
    languageOptions: { sourceType: 'commonjs' },
    rules: {
      complexity: ['warn', 0],
    },
  },
];
