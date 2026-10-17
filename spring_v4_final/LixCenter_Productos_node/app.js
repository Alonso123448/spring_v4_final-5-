'use strict';
const express = require('express');
const path = require('path');
const sequelize = require('./config/database');
const productoRoutes = require('./routes/productoRoutes');

const app = express();
const PORT = 3001; // distinto al puerto de Spring Boot (8080)

// Motor de vistas EJS
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Middlewares para leer JSON y formularios
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Rutas del módulo (vista + API)
app.use('/', productoRoutes);

// Probar conexión a la base de datos
sequelize.authenticate()
    .then(() => console.log('Conectado a SQL Server (Sistema_Marcos) desde Node.js'))
    .catch(err => console.error('Error de conexion:', err.message));

app.listen(PORT, () => {
    console.log(`Modulo LixCenter Productos (Node.js) corriendo en http://localhost:${PORT}`);
    console.log(`  - Pagina del modulo:  http://localhost:${PORT}/productos`);
    console.log(`  - API REST (JSON):    http://localhost:${PORT}/api/productos`);
});
