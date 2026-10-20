'use strict';
const { Sequelize } = require('sequelize');

// Mismos datos de conexión que application.properties (Spring Boot)
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
