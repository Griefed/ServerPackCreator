db.getSiblingDB('admin').auth(
    process.env.MONGO_INITDB_ROOT_USERNAME,
    process.env.MONGO_INITDB_ROOT_PASSWORD
);

//--------------------------------------

db = db.getSiblingDB(process.env.MONGODB_DB)
db.createCollection(process.env.MONGODB_COLLECTION)
db.createUser({
    user: process.env.MONGODB_USER,
    pwd: process.env.MONGODB_PASSWORD,
    roles: ["readWrite"],
});