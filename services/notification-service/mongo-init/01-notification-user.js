// Se ejecuta una sola vez, cuando el volumen de notification-mongodb está vacío.
// Crea el usuario propio del servicio con permisos solo sobre notification_db (paso 1.9 de la
// ruta): notification-service no puede leer ni escribir ninguna otra base.
const password = process.env.NOTIFICATION_DB_PASSWORD;
if (!password) {
  throw new Error("Falta NOTIFICATION_DB_PASSWORD");
}

db.getSiblingDB("notification_db").createUser({
  user: "notification",
  pwd: password,
  roles: [{ role: "readWrite", db: "notification_db" }],
});
