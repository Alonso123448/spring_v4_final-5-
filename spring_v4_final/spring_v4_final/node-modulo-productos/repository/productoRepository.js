const Producto = require('../models/Producto');

const getAll = () => Producto.findAll();

const getById = (id) => Producto.findByPk(id);

const create = (data) => Producto.create(data);

const update = async (id, data) => {
    const producto = await Producto.findByPk(id);
    if (!producto) return null;
    return producto.update(data);
};

const remove = async (id) => {
    const producto = await Producto.findByPk(id);
    if (!producto) return null;
    await producto.destroy();
    return true;
};

module.exports = { getAll, getById, create, update, remove };
