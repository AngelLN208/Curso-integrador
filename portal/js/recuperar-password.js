 async function solicitarRecuperacion() {
            const correo = document.getElementById('recuperar-correo').value.trim();
            if (!correo) return PortalNotify.error('Ingresa tu correo electrónico');

            const btn = document.getElementById('btn-recuperar');
            btn.disabled = true;
            btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Enviando...';

            try {
                await portalFetch('/portal/auth/recuperar-password', {
                    method: 'POST',
                    body: JSON.stringify({ correo })
                });

                document.getElementById('form-recuperar').classList.add('hidden');
                const mensaje = document.getElementById('mensaje-enviado');
                mensaje.classList.remove('hidden');
                mensaje.classList.add('flex');

            } catch (err) {
                PortalNotify.error(err.message || 'No se pudo procesar la solicitud');
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-envelope"></i> Enviar enlace de recuperación';
            }
        }