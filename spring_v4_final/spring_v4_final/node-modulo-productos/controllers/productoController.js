const productoRepository = require('../repository/productoRepository');

const listar = async (req, res) => {
    const productos = await productoRepository.getAll();
    res.json(productos);
};

const obtener = async (req, res) => {
    const producto = await productoRepository.getById(req.params.id);
    if (!producto) return res.status(404).json({ msg: 'Producto no encontrado' });
    res.json(producto);
};

const crear = async (req, res) => {
    const { nombre, precio, stock } = req.body;
    if (!nombre || nombre.trim() === '') {
        return res.status(400).json({ msg: 'El nombre es obligatorio' });
    }
    if (precio == null || precio < 0) {
        return res.status(400).json({ msg: 'El precio debe ser mayor o igual a 0' });
    }
    if (stock == null || stock < 0) {
        return res.status(400).json({ msg: 'El stock debe ser mayor o igual a 0' });
    }
    const nuevo = await productoRepository.create(req.body);
    res.status(201).json(nuevo);
};

const actualizar = async (req, res) => {
    const actualizado = await productoRepository.update(req.params.id, req.body);
    if (!actualizado) return res.status(404).json({ msg: 'Producto no encontrado' });
    res.json(actualizado);
};

const eliminar = async (req, res) => {
    const eliminado = await productoRepository.remove(req.params.id);
    if (!eliminado) return res.status(404).json({ msg: 'Producto no encontrado' });
    res.json({ msg: 'Producto eliminado' });
};

module.exports = { listar, obtener, crear, actualizar, eliminar };
