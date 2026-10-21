function cerrarSesion() {
    Swal.fire({
        title: 'Cerrar sesión',
        text: '¿Deseas cerrar tu sesión?',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí, salir',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = '/logout';
        }
    });
}