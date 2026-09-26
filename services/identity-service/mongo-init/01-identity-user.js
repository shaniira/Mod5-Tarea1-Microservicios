// Se ejecuta una sola vez, cuando el volumen de identity-mongodb está vacío. Crea el usuario
// propio del servicio con permisos solo sobre identity_db.
const password = process.env.IDENTITY_DB_PASSWORD;
if (!password) {
  throw new Error("Falta IDENTITY_DB_PASSWORD");
}

db.getSiblingDB("identity_db").createUser({
  user: "identity",
  pwd: password,
  roles: [{ role: "readWrite", db: "identity_db" }],
});
