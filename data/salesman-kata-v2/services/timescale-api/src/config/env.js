function env(name, fallback) {
  const value = process.env[name];
  return value === undefined || value === "" ? fallback : value;
}

module.exports = {
  env,
};
