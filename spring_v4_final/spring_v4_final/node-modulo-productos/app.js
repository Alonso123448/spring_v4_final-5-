const express = require('express');
const sequelize = require('./config/database');
const productoRoutes = require('./routes/productoRoutes');

const app = express();
const PORT = 3001; // distinto al puerto de Spring Boot (8080)

app.use(express.json());
app.use('/api/productos', productoRoutes);

sequelize.authenticate()
    .then(() => console.log('Conectado a SQL Server (Sistema_Marcos) desde Node.js'))
    .catch(err => console.error('Error de conexión:', err.message));

app.listen(PORT, () => {
    console.log(`Modulo Node.js de Productos corriendo en http://localhost:${PORT}`);
});
