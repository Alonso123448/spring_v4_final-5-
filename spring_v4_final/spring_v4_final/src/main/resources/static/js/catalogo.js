// =========================
// UTILIDADES
// =========================
function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// leo estuvo aqui


// Aplicar backgrounds antes de inicializar Swiper
document.querySelectorAll('.swiper-slide[data-bg]').forEach(el => {
    const url = el.getAttribute('data-bg');
    el.style.backgroundImage = `linear-gradient(135deg, rgba(37,99,235,0.7), rgba(124,58,237,0.7)), url('${url}')`;
    el.style.backgroundSize = 'cover';
    el.style.backgroundPosition = 'center';
});

const swiper = new Swiper('.heroSwiper', {

    loop: true,

    autoplay: {
        delay: 3500,
        disableOnInteraction: false,
    },

    pagination: {
        el: '.swiper-pagination',
        clickable: true,
    }

});

// =========================
// ELEMENTOS
// =========================

const openCart = document.getElementById('openCart');

const closeCart = document.getElementById('closeCart');

const cartPanel = document.getElementById('cartPanel');

const overlay = document.getElementById('overlay');

const addCartButtons = document.querySelectorAll('.add-cart');

const cartItems = document.getElementById('cartItems');

const cartCount = document.getElementById('cart-count');

const cartTotal = document.getElementById('cartTotal');

const clearCart = document.getElementById('clearCart');

const filters = document.querySelectorAll('.category-filter');

const cards = document.querySelectorAll('.product-card');

const priceRange = document.getElementById('priceRange');

const priceValue = document.getElementById('priceValue');

const searchInput = document.getElementById('searchInput');

const clearFilters = document.getElementById('clearFilters');

const toast = document.getElementById('toast');

const checkoutBtn = document.querySelector('.checkout-btn');

const productsContainer =
document.getElementById('productsContainer');

const sortSelect =
document.getElementById('sortSelect');

const stockFilter =
document.getElementById('stockFilter');

const productsFound =
document.getElementById('resultsCount');

// =========================
// PAYMENT
// =========================

const paymentModal =
document.getElementById('paymentModal');

// =========================
// VARIABLES
// =========================

let total = 0;

let count = 0;

let cart = [];

// =========================
// FUNCIONES
// =========================

function openCartPanel() {

    cartPanel.classList.add('active');

    overlay.classList.add('active');

}

function closeCartPanel() {

    cartPanel.classList.remove('active');

    overlay.classList.remove('active');

}

function updateCartUI() {

    cartTotal.textContent = `S/ ${total}`;

    cartCount.textContent = count;

}

function showToast(
    message = 'Producto agregado al carrito'
) {

    toast.innerHTML = `
        <i class="fa-solid fa-check"></i>
        ${message}
    `;

    toast.classList.add('show');

    setTimeout(() => {

        toast.classList.remove('show');

    }, 2200);

}

// =========================
// ACTUALIZAR STOCK VISUAL
// =========================

function updateStockVisual(card, stock) {

    const stockElement =
    card.querySelector('.stock');

    const button =
    card.querySelector('.add-cart');

    if (!stockElement || !button) return;

    // =========================
    // SIN STOCK
    // =========================

    if (stock <= 0) {

        stockElement.className =
        'stock out';

        stockElement.innerHTML = `

            <div class="stock-dot"></div>

            <span>
                Agotado
            </span>

        `;

        button.disabled = true;

        button.innerHTML = `

            <i class="fa-solid fa-ban"></i>

            Sin stock

        `;

        button.style.background =
        'linear-gradient(135deg,#374151,#111827)';

    }

    // =========================
    // POCO STOCK
    // =========================

    else if (stock <= 3) {

        stockElement.className =
        'stock low';

        stockElement.innerHTML = `

            <div class="stock-dot"></div>

            <span>
                Últimas ${stock} unidades
            </span>

        `;

        button.disabled = false;

    }

    // =========================
    // STOCK NORMAL
    // =========================

    else {

        stockElement.className =
        'stock available';

        stockElement.innerHTML = `

            <div class="stock-dot"></div>

            <span>
                Stock disponible: ${stock}
            </span>

        `;

        button.disabled = false;

    }

}

// =========================
// RENDER CARRITO
// =========================

function renderCart() {

    cartItems.innerHTML = '';

    total = 0;

    count = 0;

    // =========================
    // CARRITO VACIO
    // =========================

    if (cart.length === 0) {

        cartItems.innerHTML = `

            <div class="empty-cart">

                <i class="fa-solid fa-cart-shopping"></i>

                <h3>
                    Tu carrito está vacío
                </h3>

                <p>
                    Agrega productos increíbles
                </p>

            </div>

        `;

    }

    // =========================
    // RECORRER PRODUCTOS
    // =========================

    cart.forEach((product, index) => {

        total += product.price * product.quantity;

        count += product.quantity;

        const item = document.createElement('div');

        item.classList.add('cart-item');

        const safeName = escapeHtml(product.name);
        const imgHtml = product.img
            ? `<img src="${escapeHtml(product.img)}" alt="${safeName}" style="width:52px;height:52px;border-radius:10px;object-fit:cover;flex-shrink:0;border:1px solid rgba(255,255,255,.1);">`
            : `<div style="width:52px;height:52px;border-radius:10px;background:rgba(99,102,241,.15);display:flex;align-items:center;justify-content:center;flex-shrink:0;"><i class="fa-solid fa-box" style="color:#6366f1;font-size:18px;"></i></div>`;

        item.innerHTML = `
            <div style="display:flex;gap:12px;align-items:flex-start;">
                ${imgHtml}
                <div style="flex:1;min-width:0;">
                    <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;">
                        <h4 style="font-size:13px;font-weight:700;color:#e2e8f0;line-height:1.3;margin:0;">${safeName}</h4>
                        <button class="remove-item" title="Eliminar" style="flex-shrink:0;width:28px;height:28px;border-radius:8px;background:rgba(239,68,68,.15);color:#f87171;border:none;cursor:pointer;display:flex;align-items:center;justify-content:center;font-size:11px;transition:.2s;">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                    <div style="font-size:14px;font-weight:800;color:#34d399;margin:4px 0 6px;">S/ ${product.price.toFixed(2)}</div>
                    <div style="display:flex;align-items:center;justify-content:space-between;">
                        <small style="font-size:10px;color:#475569;">Stock: ${product.stock}</small>
                        <div class="quantity-controls" style="margin:0;gap:6px;">
                            <button class="qty-btn minus-btn" style="width:26px;height:26px;border-radius:7px;font-size:14px;font-weight:700;">−</button>
                            <span class="qty-number" style="font-size:14px;font-weight:800;color:white;min-width:18px;text-align:center;">${product.quantity}</span>
                            <button class="qty-btn plus-btn" style="width:26px;height:26px;border-radius:7px;font-size:14px;font-weight:700;">+</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        cartItems.appendChild(item);

        // =========================
        // ELIMINAR
        // =========================

        const removeBtn =
        item.querySelector('.remove-item');

        removeBtn.addEventListener('click', () => {

            // El stock del catálogo no se toca (nunca fue descontado)
            cart.splice(index, 1);

            renderCart();

            saveCart();

            showToast('Producto eliminado');

        });

        // =========================
        // SUMAR CANTIDAD
        // =========================

        const plusBtn =
        item.querySelector('.plus-btn');

        plusBtn.addEventListener('click', () => {

            if (product.stock <= 0) {

                showToast(
                    'No hay más stock'
                );

                return;

            }

            // Solo se actualiza el carrito; el stock del catálogo
            // no cambia hasta que se emita la nota de venta
            product.quantity++;

            product.stock--;

            renderCart();

            saveCart();

        });

        // =========================
        // RESTAR CANTIDAD
        // =========================

        const minusBtn =
        item.querySelector('.minus-btn');

        minusBtn.addEventListener('click', () => {

            // Solo se actualiza el carrito; el stock del catálogo no cambia
            product.quantity--;

            product.stock++;

            if (product.quantity <= 0) {

                cart.splice(index, 1);

            }

            renderCart();

            saveCart();

        });

    });

    updateCartUI();

}

// =========================
// GUARDAR CARRITO
// =========================

function saveCart() {

    localStorage.setItem(
        'lixgame-cart',
        JSON.stringify(cart)
    );

}

// =========================
// CARGAR CARRITO
// =========================

function loadCart() {

    const savedCart =
    localStorage.getItem(
        'lixgame-cart'
    );

    if (savedCart) {

        cart = JSON.parse(savedCart);

        // Sincronizar cada item con el stock REAL del servidor
        // (el dataset.stock viene fresco de la BD en cada carga de página)
        cart.forEach(item => {
            const btn = document.querySelector(`.add-cart[data-id="${item.id}"]`);
            if (btn) {
                if (!item.img) item.img = btn.dataset.img || '';
                const stockReal = parseInt(btn.dataset.stock) || 0;
                // Si pidió más de lo que hay ahora, ajustar la cantidad
                if (item.quantity > stockReal) item.quantity = stockReal;
                item.stock = stockReal - item.quantity;
            }
        });

        // Quitar items que quedaron en 0 (producto agotado)
        cart = cart.filter(item => item.quantity > 0);

        renderCart();

    }

}

// =========================
// ABRIR / CERRAR CARRITO
// =========================

if (openCart) {

    openCart.addEventListener(
        'click',
        openCartPanel
    );

}

if (closeCart) {

    closeCart.addEventListener(
        'click',
        closeCartPanel
    );

}

if (overlay) {

    overlay.addEventListener(
        'click',
        closeCartPanel
    );

}

// =========================
// AGREGAR AL CARRITO
// =========================

addCartButtons.forEach(button => {

    button.addEventListener('click', () => {

        const id    = parseInt(button.dataset.id);
        const name  = button.dataset.name;
        const price = parseFloat(button.dataset.price);
        const img   = button.dataset.img || '';
        const stock = parseInt(button.dataset.stock); // stock real (no se modifica hasta emitir la venta)

        if (stock <= 0) {
            showToast('Producto agotado');
            return;
        }

        const existingProduct = cart.find(p => p.id === id);
        const enCarrito = existingProduct ? existingProduct.quantity : 0;

        // Validar contra el stock real sin descontarlo visualmente
        if (enCarrito >= stock) {
            showToast('No hay más stock disponible');
            return;
        }

        if (existingProduct) {
            existingProduct.quantity++;
            existingProduct.stock = stock - existingProduct.quantity;
        } else {
            cart.push({ id, name, price, img, stock: stock - 1, quantity: 1 });
        }

        renderCart();

        saveCart();

        openCartPanel();

        showToast();

        button.innerHTML = `

            <i class="fa-solid fa-check"></i>

            Agregado

        `;

        button.style.background =
        'linear-gradient(135deg,#10b981,#34d399)';

        setTimeout(() => {

            if (!button.disabled) {

                button.innerHTML = `

                    <i class="fa-solid fa-cart-shopping"></i>

                    Agregar al carrito

                `;

                button.style.background =
                'linear-gradient(135deg,#2563eb,#7c3aed)';

            }

        }, 1200);

    });

});

// =========================
// VACIAR CARRITO
// =========================

if (clearCart) {

    clearCart.addEventListener('click', () => {

        // El stock del catálogo no se toca (nunca fue descontado)
        cart = [];

        renderCart();

        saveCart();

        showToast('Carrito vaciado');

    });

}

// =========================
// FINALIZAR COMPRA
// =========================

if (checkoutBtn) {
    checkoutBtn.addEventListener('click', () => {
        if (cart.length === 0) { showToast('Tu carrito está vacío'); return; }
        abrirModalPago();
    });
}

// ── Config de pago ──
let yapeNumero = '';
let yapeNombre = '';
let transBanco = '';
let transCuenta = '';
let transCci = '';

function initCheckoutConfig() {
    const cfg = document.getElementById('checkoutConfig');
    if (!cfg) return;
    yapeNumero = cfg.dataset.yape   || '';
    yapeNombre = cfg.dataset.nombre || '';
    transBanco  = cfg.dataset.banco  || '';
    transCuenta = cfg.dataset.cuenta || '';
    transCci    = cfg.dataset.cci    || '';

    const numEl = document.getElementById('yapeNumDisplay');
    const nomEl = document.getElementById('yapeNomDisplay');
    const qrPh  = document.getElementById('qrPhoneDisplay');
    const qrImg = document.getElementById('qrCodeImg');
    if (numEl) numEl.textContent = yapeNumero.replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3');
    if (nomEl) { const sp = nomEl.querySelector('span'); if (sp) sp.textContent = yapeNombre; }
    if (qrPh)  qrPh.textContent  = yapeNumero.replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3');
    // Usar QR real de Yape si está disponible, si no fallback al generado
    const yapeQr = cfg.dataset.qr || '';
    if (qrImg) {
        if (yapeQr) {
            qrImg.src = yapeQr;
            qrImg.onerror = () => {
                // Si la imagen no existe, mostrar placeholder
                qrImg.style.display = 'none';
                const ph = document.getElementById('qrPlaceholder');
                if (ph) ph.style.display = 'flex';
            };
            qrImg.onload = () => {
                qrImg.style.display = 'block';
                const ph = document.getElementById('qrPlaceholder');
                if (ph) ph.style.display = 'none';
            };
        } else if (yapeNumero) {
            qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${yapeNumero}`;
        }
    }
    const bEl = document.getElementById('transBancoDisplay');
    const cEl = document.getElementById('transCuentaDisplay');
    const iEl = document.getElementById('transCciDisplay');
    if (bEl) bEl.textContent = transBanco;
    if (cEl) cEl.textContent = transCuenta;
    if (iEl) iEl.textContent = transCci;
}

function abrirModalPago() {
    initCheckoutConfig();

    // Reset campos
    ['payDoc','payNombre','payCelular','payCorreo','payDireccion',
     'payOperacion','payOperacionTrans'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    document.getElementById('voucherInput').value = '';
    document.getElementById('voucherInputTrans').value = '';

    // Reset errores
    ['errDoc','errNombre','errCelular','errCorreo','errDireccion',
     'errOperacion','errOperacionTrans'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });

    // Reset entrega y método
    toggleEntrega('DOMICILIO');
    toggleMetodo('YAPE');
    document.querySelector('input[name="entrega"][value="DOMICILIO"]').checked = true;
    document.querySelector('input[name="metodoPago"][value="YAPE"]').checked = true;

    llenarResumen();
    mostrarOverlay(null);

    const modal = document.getElementById('paymentModal');
    modal.style.display = 'block';
    modal.scrollTop = 0;
    closeCartPanel();
}

function llenarResumen() {
    const total = cart.reduce((s, p) => s + p.price * p.quantity, 0);
    const sub = document.getElementById('resSubtotal');
    const tot = document.getElementById('resTotal');
    const badge = document.getElementById('resumenBadge');
    if (sub) sub.textContent = 'S/ ' + total.toFixed(2);
    if (tot) tot.textContent = 'S/ ' + total.toFixed(2);
    if (badge) badge.textContent = cart.length + (cart.length === 1 ? ' Item' : ' Items');

    const container = document.getElementById('payResumen');
    if (container) {
        container.innerHTML = cart.map(p => `
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
                <div style="width:48px;height:48px;border-radius:10px;background:#f3f4f6;
                            display:flex;align-items:center;justify-content:center;
                            font-size:20px;flex-shrink:0;">📦</div>
                <div style="flex:1;">
                    <div style="font-size:13px;font-weight:700;color:#1f2937;">${p.name}</div>
                    <div style="font-size:12px;color:#9ca3af;">Cant: ${p.quantity} × S/ ${p.price.toFixed(2)}</div>
                </div>
                <div style="font-size:14px;font-weight:800;color:#7c3aed;">S/ ${(p.price * p.quantity).toFixed(2)}</div>
            </div>
        `).join('');
    }
}

function cerrarModalPago() {
    document.getElementById('paymentModal').style.display = 'none';
    const dd = document.getElementById('dniDropdown');
    if (dd) dd.style.display = 'none';
}

function toggleEntrega(tipo) {
    const isDomicilio = tipo === 'DOMICILIO';
    const stepDir = document.getElementById('stepDireccion');
    const numPaso = document.getElementById('numPaso4');
    if (stepDir) stepDir.style.display = isDomicilio ? 'block' : 'none';
    if (numPaso) numPaso.textContent = isDomicilio ? '4' : '3';
    document.getElementById('optDomicilio').classList.toggle('active', isDomicilio);
    document.getElementById('optTienda').classList.toggle('active', !isDomicilio);
}

function toggleMetodo(metodo) {
    document.getElementById('secYape').style.display          = metodo === 'YAPE' ? 'block' : 'none';
    document.getElementById('secTransferencia').style.display = metodo === 'TRANSFERENCIA' ? 'block' : 'none';
    document.getElementById('optYape').classList.toggle('active', metodo === 'YAPE');
    document.getElementById('optTransferencia').classList.toggle('active', metodo === 'TRANSFERENCIA');
}

// ── QR Modal ──
function abrirQR() {
    document.getElementById('qrModal').style.display = 'flex';
}
function cerrarQR() {
    document.getElementById('qrModal').style.display = 'none';
}
function copiarNumYape() {
    navigator.clipboard.writeText(yapeNumero).then(() => {
        const btn = document.getElementById('btnCopiarYape');
        if (btn) {
            btn.textContent = '✅ Copiado';
            setTimeout(() => { btn.textContent = '📋 Copiar Número'; }, 2000);
        }
    });
}

// ── DNI Autocomplete ──
let dniDebounce = null;
function onDocInput(input) {
    const val = input.value;
    const dd = document.getElementById('dniDropdown');
    clearTimeout(dniDebounce);
    if (val.length < 2) { dd.style.display = 'none'; return; }
    dniDebounce = setTimeout(async () => {
        try {
            const r = await fetch(`/catalogo/clientes/buscar?q=${encodeURIComponent(val)}`);
            const data = await r.json();
            if (!data.length) { dd.style.display = 'none'; return; }
            dd.innerHTML = data.map(c =>
                `<div class="dni-item" onclick="seleccionarCliente('${c.doc}','${c.nombre.replace(/'/g,"\\'")}')">
                    <span class="dni-item-num">${c.doc}</span>
                    <span class="dni-item-name">${c.nombre}</span>
                 </div>`
            ).join('');
            dd.style.display = 'block';
        } catch(e) { dd.style.display = 'none'; }
    }, 280);
}

function seleccionarCliente(doc, nombre) {
    document.getElementById('payDoc').value    = doc;
    document.getElementById('payNombre').value = nombre;
    document.getElementById('dniDropdown').style.display = 'none';
}

async function buscarDoc() {
    const doc = document.getElementById('payDoc').value.trim();
    if (doc.length !== 8 && doc.length !== 11) {
        document.getElementById('errDoc').style.display = 'block'; return;
    }
    try {
        const r = await fetch(`/ventas/api/consultar?documento=${doc}`);
        const d = await r.json();
        if (d.success && d.nombre) {
            document.getElementById('payNombre').value = d.nombre;
            document.getElementById('errDoc').style.display = 'none';
        }
    } catch(e) {}
}

document.addEventListener('click', e => {
    if (!e.target.closest('#payDoc') && !e.target.closest('#dniDropdown')) {
        const dd = document.getElementById('dniDropdown');
        if (dd) dd.style.display = 'none';
    }
});

function mostrarOverlay(id) {
    ['stepProcesando','stepExito'].forEach(s => {
        const el = document.getElementById(s);
        if (el) el.style.display = (s === id) ? 'flex' : 'none';
    });
}

async function confirmarPedido() {
    const doc      = document.getElementById('payDoc').value.trim();
    const nombre   = document.getElementById('payNombre').value.trim();
    const celular  = document.getElementById('payCelular').value.trim();
    const correo   = document.getElementById('payCorreo').value.trim();
    const entrega  = document.querySelector('input[name="entrega"]:checked')?.value || 'DOMICILIO';
    const direccion = document.getElementById('payDireccion').value.trim();
    const metodo   = document.querySelector('input[name="metodoPago"]:checked')?.value || 'YAPE';
    const operacion = metodo === 'YAPE'
        ? document.getElementById('payOperacion').value.trim()
        : document.getElementById('payOperacionTrans').value.trim();
    const voucherFile = metodo === 'YAPE'
        ? document.getElementById('voucherInput').files[0]
        : document.getElementById('voucherInputTrans').files[0];

    // Reset errores
    ['errDoc','errNombre','errCelular','errCorreo','errDireccion',
     'errOperacion','errOperacionTrans'].forEach(id => {
        document.getElementById(id).style.display = 'none';
    });

    let ok = true;
    if (doc.length !== 8 && doc.length !== 11) {
        document.getElementById('errDoc').style.display = 'block'; ok = false;
    }
    if (nombre.length < 3) {
        document.getElementById('errNombre').style.display = 'block'; ok = false;
    }
    if (celular.length !== 9 || celular[0] !== '9') {
        document.getElementById('errCelular').style.display = 'block'; ok = false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
        document.getElementById('errCorreo').style.display = 'block'; ok = false;
    }
    if (entrega === 'DOMICILIO' && direccion.length < 4) {
        document.getElementById('errDireccion').style.display = 'block'; ok = false;
    }
    if (!operacion) {
        document.getElementById(metodo === 'YAPE' ? 'errOperacion' : 'errOperacionTrans').style.display = 'block';
        ok = false;
    }
    if (!ok) return;

    // Verificar unicidad del código
    try {
        const r = await fetch(`/catalogo/verificar-codigo?codigo=${encodeURIComponent(operacion)}`);
        const d = await r.json();
        if (!d.disponible) {
            const errId = metodo === 'YAPE' ? 'errOperacion' : 'errOperacionTrans';
            const errEl = document.getElementById(errId);
            errEl.textContent = 'Este código ya fue registrado. Verifica tu comprobante.';
            errEl.style.display = 'block';
            return;
        }
    } catch(e) {}

    mostrarOverlay('stepProcesando');
    const msgs = ['Verificando datos...','Procesando pedido...','Registrando compra...','Casi listo...'];
    let mi = 0;
    const iv = setInterval(() => {
        const el = document.getElementById('procesandoMsg');
        if (el) el.textContent = msgs[mi % msgs.length]; mi++;
    }, 900);

    const fd = new FormData();
    fd.append('clienteNombre',    nombre);
    fd.append('clienteDocumento', doc);
    fd.append('clienteTelefono',  celular);
    fd.append('clienteCorreo',    correo);
    fd.append('tipoCompra',       'CONTADO');
    fd.append('metodoPago',       metodo);
    fd.append('tipoEntrega',      entrega);
    fd.append('direccionEntrega', direccion);
    fd.append('codigoOperacion',  operacion);
    if (voucherFile) fd.append('voucher', voucherFile);
    cart.forEach(p => {
        fd.append('productosId',       p.id);
        fd.append('productosNombre',   p.name);
        fd.append('productosPrecio',   p.price);
        fd.append('productosCantidad', p.quantity);
    });

    try {
        await new Promise(r => setTimeout(r, 1800));
        const res  = await fetch('/catalogo/pedido', { method: 'POST', body: fd });
        const data = await res.json();
        clearInterval(iv);

        if (data.ok) {
            document.getElementById('exitoNumPedido').textContent = data.pedido;
            document.getElementById('exitoTotal').textContent     = 'S/ ' + parseFloat(data.total).toFixed(2);
            document.getElementById('exitoAprobacion').innerHTML  =
                `<span style="background:#fef3c7;color:#92400e;padding:3px 10px;
                 border-radius:10px;font-size:11px;font-weight:800;">
                 ⏳ EN REVISIÓN — pendiente de aprobación</span>`;
            mostrarOverlay('stepExito');
            cart = []; renderCart(); saveCart();
        } else {
            mostrarOverlay(null);
            alert(data.msg || 'No se pudo procesar el pedido.');
        }
    } catch(e) {
        clearInterval(iv);
        mostrarOverlay(null);
        alert('Error de conexión. Inténtalo nuevamente.');
    }
}

function cerrarExito() {
    document.getElementById('paymentModal').style.display = 'none';
    mostrarOverlay(null);
}

// ── Detectar ubicación GPS + Nominatim reverse geocoding ──
async function detectarUbicacion() {
    const btn     = document.getElementById('btnGps');
    const icon    = document.getElementById('gpsIcon');
    const txt     = document.getElementById('gpsTxt');
    const errEl   = document.getElementById('errGps');
    const errTxt  = document.getElementById('errGpsTxt');
    const preview = document.getElementById('dirPreview');

    if (!navigator.geolocation) {
        errTxt.textContent = 'Tu navegador no soporta geolocalización.';
        errEl.style.display = 'flex';
        return;
    }

    // Estado cargando
    errEl.style.display = 'none';
    icon.className = 'fa-solid fa-spinner fa-spin';
    txt.textContent = 'Detectando...';
    btn.disabled = true;

    navigator.geolocation.getCurrentPosition(
        async (pos) => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            try {
                const r = await fetch(
                    `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&addressdetails=1`,
                    { headers: { 'Accept-Language': 'es' } }
                );
                const data = await r.json();

                // Armar dirección legible
                const a = data.address || {};
                const partes = [
                    a.road || a.pedestrian || a.footway || '',
                    a.house_number || '',
                    a.suburb || a.neighbourhood || a.quarter || '',
                    a.city || a.town || a.village || a.municipality || '',
                    a.state || ''
                ].filter(Boolean);
                const direccion = partes.join(', ');

                document.getElementById('payDireccion').value = direccion;

                // Preview con link a Google Maps
                document.getElementById('dirPreviewTxt').textContent = data.display_name || direccion;
                document.getElementById('dirMapsLink').href =
                    `https://www.google.com/maps?q=${lat},${lng}`;
                preview.style.display = 'block';

                // Quitar error de dirección si estaba
                document.getElementById('errDireccion').style.display = 'none';

                // Restaurar botón — estado éxito
                icon.className = 'fa-solid fa-circle-check';
                icon.style.color = '#34d399';
                txt.textContent = 'Ubicación detectada';
                setTimeout(() => {
                    icon.className = 'fa-solid fa-crosshairs';
                    icon.style.color = '';
                    txt.textContent = 'Usar ubicación';
                    btn.disabled = false;
                }, 3000);

            } catch (e) {
                icon.className = 'fa-solid fa-crosshairs';
                txt.textContent = 'Usar ubicación';
                btn.disabled = false;
                errTxt.textContent = 'No se pudo obtener la dirección. Escríbela manualmente.';
                errEl.style.display = 'flex';
            }
        },
        (err) => {
            icon.className = 'fa-solid fa-crosshairs';
            txt.textContent = 'Usar ubicación';
            btn.disabled = false;
            const msgs = {
                1: 'Permiso de ubicación denegado. Actívalo en tu navegador.',
                2: 'No se pudo detectar tu posición. Inténtalo de nuevo.',
                3: 'Tiempo de espera agotado. Inténtalo de nuevo.'
            };
            errTxt.textContent = msgs[err.code] || 'Error al obtener ubicación.';
            errEl.style.display = 'flex';
        },
        { timeout: 10000, enableHighAccuracy: true }
    );
}

function previewVoucher(input, labelId) {
    const el = document.getElementById(labelId);
    if (!el) return;
    if (input.files && input.files[0]) {
        el.textContent = input.files[0].name.length > 20
            ? input.files[0].name.substring(0, 20) + '...'
            : input.files[0].name;
    }
}

// =========================
// FILTRAR PRODUCTOS
// =========================

function filterProducts() {

    const selectedCategories =
        Array.from(filters)
            .filter(f => f.checked)
            .map(f => f.value);

    const maxPrice = parseInt(priceRange.value);

    const search =
    searchInput.value.toLowerCase();

    const stockOnly =
    stockFilter.checked;

    let visibleCards = [];

    cards.forEach(card => {

        const category =
        card.dataset.category;

        const price =
        parseInt(card.dataset.price);

        const stock =
        parseInt(card.dataset.stock);

        const title =
        card.querySelector('h3')
        .textContent
        .toLowerCase();

        const matchCategory =
            selectedCategories.length === 0 ||
            selectedCategories.includes(category);

        const matchPrice =
        price <= maxPrice;

        const matchSearch =
        title.includes(search);

        const matchStock =
        !stockOnly || stock > 0;

        if (
            matchCategory &&
            matchPrice &&
            matchSearch &&
            matchStock
        ) {

            card.style.display = '';

            visibleCards.push(card);

        }

        else {

            card.style.display = 'none';

        }

    });

    if (productsFound) {

        productsFound.textContent =
        `${visibleCards.length} productos encontrados`;

    }

    sortProductsCards(visibleCards);

}

// =========================
// ORDENAR PRODUCTOS
// =========================

function sortProductsCards(
    visibleCards = null
) {

    if (!sortSelect) return;

    const list =
    visibleCards || Array.from(cards);

    list.sort((a, b) => {

        const priceA =
        parseInt(a.dataset.price);

        const priceB =
        parseInt(b.dataset.price);

        const stockA =
        parseInt(a.dataset.stock);

        const stockB =
        parseInt(b.dataset.stock);

        const titleA =
        a.querySelector('h3').textContent;

        const titleB =
        b.querySelector('h3').textContent;

        switch (sortSelect.value) {

            case 'price-low':
                return priceA - priceB;

            case 'price-high':
                return priceB - priceA;

            case 'stock':
                return stockB - stockA;

            case 'name':
                return titleA.localeCompare(titleB);

            default:
                return 0;

        }

    });

    list.forEach(card => {

        productsContainer.appendChild(card);

    });

}

// =========================
// EVENTOS FILTROS
// =========================

filters.forEach(filter => {

    filter.addEventListener(
        'change',
        filterProducts
    );

});

// =========================
// RANGE PRECIO
// =========================

if (priceRange) {

    priceRange.addEventListener('input', () => {

        priceValue.textContent =
        `S/ ${priceRange.value}`;

        filterProducts();

    });

}

// =========================
// BUSCADOR
// =========================

if (searchInput) {

    searchInput.addEventListener(
        'input',
        filterProducts
    );

}

// =========================
// FILTRO STOCK
// =========================

if (stockFilter) {

    stockFilter.addEventListener(
        'change',
        filterProducts
    );

}

// =========================
// ORDENAR
// =========================

if (sortSelect) {

    sortSelect.addEventListener(
        'change',
        () => {

            sortProductsCards();

            filterProducts();

        }
    );

}

// =========================
// LIMPIAR FILTROS
// =========================

if (clearFilters) {

    clearFilters.addEventListener('click', () => {

        filters.forEach(filter => {

            filter.checked = false;

        });

        if (stockFilter) {

            stockFilter.checked = false;

        }

        if (sortSelect) {

            sortSelect.value = 'default';

        }

        priceRange.value = 7000;

        priceValue.textContent =
        'S/ 7000';

        searchInput.value = '';

        filterProducts();

        sortProductsCards();

        showToast('Filtros limpiados');

    });

}

// =========================
// SCROLL SUAVE
// =========================

document.querySelectorAll(
    'a[href^="#"]'
).forEach(anchor => {

    anchor.addEventListener(
        'click',
        function (e) {

            const target =
            document.querySelector(
                this.getAttribute('href')
            );

            if (target) {

                e.preventDefault();

                target.scrollIntoView({

                    behavior: 'smooth'

                });

            }

        }
    );

});

// =========================
// INICIALIZAR STOCK
// =========================

cards.forEach(card => {

    const stock =
    parseInt(card.dataset.stock);

    updateStockVisual(card, stock);

});

// =========================
// INICIALIZAR
// =========================

loadCart();

updateCartUI();

filterProducts();

sortProductsCards();
