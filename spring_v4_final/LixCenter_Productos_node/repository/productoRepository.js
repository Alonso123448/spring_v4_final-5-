'use strict';
const Producto = require('../models/Producto');

const getAll = async () => {
    return await Producto.findAll();
};

const getById = async (id) => {
    return await Producto.findByPk(id);
};

const create = async (data) => {
    return await Producto.create(data);
};

const update = async (id, data) => {
    return await Producto.update(data, { where: { id } });
};

const remove = async (id) => {
    return await Producto.destroy({ where: { id } });
};

module.exports = { getAll, getById, create, update, remove };
