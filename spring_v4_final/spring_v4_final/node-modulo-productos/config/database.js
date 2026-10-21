const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('Sistema_Marcos', 'sa', '215295', {
    host: 'localhost',
    port: 1433,
    dialect: 'mssql',
    dialectOptions: {
        options: {
            encrypt: false,
            trustServerCertificate: true
        }
    },
    logging: false
});

module.exports = sequelize;
