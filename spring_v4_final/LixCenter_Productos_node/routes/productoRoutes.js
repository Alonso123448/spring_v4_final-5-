'use strict';
const express = require('express');
const router = express.Router();
const controller = require('../controllers/productoController');

// ── Vista (página del módulo) ──
router.get('/productos', controller.vista);

// ── API REST ──
router.get('/api/productos', controller.listar);
router.get('/api/productos/:id', controller.obtener);
router.post('/api/productos', controller.crear);
router.put('/api/productos/:id', controller.actualizar);
router.delete('/api/productos/:id', controller.eliminar);

module.exports = router;
