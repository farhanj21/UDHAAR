module.exports = {
  env: {
    es2021: true,
    node: true,
    commonjs: true,
  },
  parserOptions: {
    ecmaVersion: 12,
    sourceType: "script",
  },
  extends: [
    "eslint:recommended",
  ],
  rules: {
    "no-unused-vars": "warn",
    "no-undef": "error",
    "quotes": ["error", "double", { "allowTemplateLiterals": true }]
  },
  globals: {
    require: "readonly",
    module: "readonly",
    exports: "readonly"
  }
};
