const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Producto = sequelize.define('Producto', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    nombre: DataTypes.STRING,
    detalle: DataTypes.STRING,
    precio: DataTypes.DECIMAL(10, 2),
    stock: DataTypes.INTEGER,
    estado: DataTypes.BOOLEAN,
    stockMinimo: {
        type: DataTypes.INTEGER,
        field: 'stock_minimo'
    },
    fechaVencimiento: {
        type: DataTypes.DATEONLY,
        field: 'fecha_vencimiento'
    }
}, {
    tableName: 'productos',
    timestamps: false
});

module.exports = Producto;
