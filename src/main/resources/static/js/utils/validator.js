const Validators = {

    // ─── Helpers base ────────────────────────────────────────
    estaVacio: (v) => !v || v.toString().trim() === '',

    esDNIvalido: (v) => /^\d{8}$/.test(v),

    esEmailValido: (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),

    esFechaFutura: (isoString) => new Date(isoString) > new Date(),

    esFechaPasada: (isoString) => new Date(isoString) < new Date(),

    // Muestra error bajo el campo
    marcarError: (campoId, mensaje) => {
        const campo = document.getElementById(campoId);
        if (!campo) return;
        campo.classList.add('is-invalid');
        let feedback = campo.nextElementSibling;
        if (!feedback || !feedback.classList.contains('invalid-feedback')) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            campo.insertAdjacentElement('afterend', feedback);
        }
        feedback.textContent = mensaje;
    },

    limpiarErrores: (ids) => {
        ids.forEach(id => {
            const campo = document.getElementById(id);
            if (campo) campo.classList.remove('is-invalid');
        });
    },

    // ─── PacienteRequest ─────────────────────────────────────
    // Refleja: dni, nombres, apellidos, fechaNacimiento,
    //          celular, correo, sexo
    paciente: (form) => {
        const errores = [];
        Validators.limpiarErrores([
            'pac-dni','pac-nombres','pac-apellidos',
            'pac-fechaNacimiento','pac-celular','pac-correo','pac-sexo'
        ]);

        if (Validators.estaVacio(form.dni)) {
            Validators.marcarError('pac-dni', 'El DNI es obligatorio');
            errores.push('dni');
        } else if (!Validators.esDNIvalido(form.dni)) {
            Validators.marcarError('pac-dni', 'El DNI debe tener exactamente 8 dígitos');
            errores.push('dni');
        }

        if (Validators.estaVacio(form.nombres)) {
            Validators.marcarError('pac-nombres', 'Los nombres son obligatorios');
            errores.push('nombres');
        }

        if (Validators.estaVacio(form.apellidos)) {
            Validators.marcarError('pac-apellidos', 'Los apellidos son obligatorios');
            errores.push('apellidos');
        }

        if (Validators.estaVacio(form.fechaNacimiento)) {
            Validators.marcarError('pac-fechaNacimiento', 'La fecha de nacimiento es obligatoria');
            errores.push('fechaNacimiento');
        } else if (!Validators.esFechaPasada(form.fechaNacimiento)) {
            Validators.marcarError('pac-fechaNacimiento', 'La fecha de nacimiento debe ser en el pasado');
            errores.push('fechaNacimiento');
        }

        if (Validators.estaVacio(form.celular)) {
            Validators.marcarError('pac-celular', 'El celular es obligatorio');
            errores.push('celular');
        }

        if (!Validators.estaVacio(form.correo) && !Validators.esEmailValido(form.correo)) {
            Validators.marcarError('pac-correo', 'El correo debe tener un formato válido');
            errores.push('correo');
        }

        if (Validators.estaVacio(form.sexo)) {
            Validators.marcarError('pac-sexo', 'El sexo es obligatorio');
            errores.push('sexo');
        } else if (!/^[MF]$/.test(form.sexo)) {
            Validators.marcarError('pac-sexo', 'El sexo debe ser M o F');
            errores.push('sexo');
        }

        return errores.length === 0;
    },

    // ─── CitaRequest ─────────────────────────────────────────
    // Refleja: pacienteId, medicoId, fechaHora (future),
    //          motivo (opcional), seguroId (opcional)
    cita: (form) => {
        const errores = [];
        Validators.limpiarErrores([
            'cita-pacienteId','cita-medicoId','cita-fechaHora'
        ]);

        if (Validators.estaVacio(form.pacienteId)) {
            Validators.marcarError('cita-pacienteId', 'El paciente es obligatorio');
            errores.push('pacienteId');
        }

        if (Validators.estaVacio(form.medicoId)) {
            Validators.marcarError('cita-medicoId', 'El médico es obligatorio');
            errores.push('medicoId');
        }

        if (Validators.estaVacio(form.fechaHora)) {
            Validators.marcarError('cita-fechaHora', 'La fecha y hora son obligatorias');
            errores.push('fechaHora');
        } else if (!Validators.esFechaFutura(form.fechaHora)) {
            Validators.marcarError('cita-fechaHora', 'La fecha de la cita debe ser futura');
            errores.push('fechaHora');
        }

        return errores.length === 0;
    },

    // ─── CitaReprogramarRequest ───────────────────────────────
    // Refleja: nuevaFechaHora (future, not null)
    reprogramarCita: (form) => {
        const errores = [];
        Validators.limpiarErrores(['reprog-fechaHora']);

        if (Validators.estaVacio(form.nuevaFechaHora)) {
            Validators.marcarError('reprog-fechaHora', 'La nueva fecha y hora son obligatorias');
            errores.push('nuevaFechaHora');
        } else if (!Validators.esFechaFutura(form.nuevaFechaHora)) {
            Validators.marcarError('reprog-fechaHora', 'La nueva fecha debe ser futura');
            errores.push('nuevaFechaHora');
        }

        return errores.length === 0;
    },

    // ─── LoginRequest ─────────────────────────────────────────
    // Refleja: username (notBlank), password (notBlank)
    login: (form) => {
        const errores = [];
        Validators.limpiarErrores(['login-username', 'login-password']);

        if (Validators.estaVacio(form.username)) {
            Validators.marcarError('login-username', 'El username es obligatorio');
            errores.push('username');
        }

        if (Validators.estaVacio(form.password)) {
            Validators.marcarError('login-password', 'La contraseña es obligatoria');
            errores.push('password');
        }

        return errores.length === 0;
    },

    // ─── MedicoRequest ────────────────────────────────────────
    // Refleja: dni, nombres, apellidos, especialidadId,
    //          celular, correo, username (email), password
    medico: (form) => {
        const errores = [];
        Validators.limpiarErrores([
            'med-dni','med-nombres','med-apellidos','med-especialidadId',
            'med-celular','med-correo','med-username','med-password'
        ]);

        if (Validators.estaVacio(form.dni)) {
            Validators.marcarError('med-dni', 'El DNI es obligatorio');
            errores.push('dni');
        } else if (!Validators.esDNIvalido(form.dni)) {
            Validators.marcarError('med-dni', 'El DNI debe tener exactamente 8 dígitos');
            errores.push('dni');
        }

        if (Validators.estaVacio(form.nombres)) {
            Validators.marcarError('med-nombres', 'Los nombres son obligatorios');
            errores.push('nombres');
        }

        if (Validators.estaVacio(form.apellidos)) {
            Validators.marcarError('med-apellidos', 'Los apellidos son obligatorios');
            errores.push('apellidos');
        }

        if (Validators.estaVacio(form.especialidadId)) {
            Validators.marcarError('med-especialidadId', 'La especialidad es obligatoria');
            errores.push('especialidadId');
        }

        if (Validators.estaVacio(form.celular)) {
            Validators.marcarError('med-celular', 'El celular es obligatorio');
            errores.push('celular');
        }

        if (Validators.estaVacio(form.correo)) {
            Validators.marcarError('med-correo', 'El correo es obligatorio');
            errores.push('correo');
        } else if (!Validators.esEmailValido(form.correo)) {
            Validators.marcarError('med-correo', 'El correo debe tener un formato válido');
            errores.push('correo');
        }

        if (Validators.estaVacio(form.username)) {
            Validators.marcarError('med-username', 'El username es obligatorio');
            errores.push('username');
        } else if (!Validators.esEmailValido(form.username)) {
            Validators.marcarError('med-username', 'El username debe ser un correo válido');
            errores.push('username');
        }

        if (Validators.estaVacio(form.password)) {
            Validators.marcarError('med-password', 'La contraseña es obligatoria');
            errores.push('password');
        }

        return errores.length === 0;
    },

    // ─── HorarioRequest ───────────────────────────────────────
    // Refleja: medicoId, dia (DayOfWeek), horaInicio, horaFin
    horario: (form) => {
        const errores = [];
        Validators.limpiarErrores([
            'hor-medicoId','hor-dia','hor-horaInicio','hor-horaFin'
        ]);

        if (Validators.estaVacio(form.medicoId)) {
            Validators.marcarError('hor-medicoId', 'El médico es obligatorio');
            errores.push('medicoId');
        }

        if (Validators.estaVacio(form.dia)) {
            Validators.marcarError('hor-dia', 'El día es obligatorio');
            errores.push('dia');
        }

        if (Validators.estaVacio(form.horaInicio)) {
            Validators.marcarError('hor-horaInicio', 'La hora de inicio es obligatoria');
            errores.push('horaInicio');
        }

        if (Validators.estaVacio(form.horaFin)) {
            Validators.marcarError('hor-horaFin', 'La hora de fin es obligatoria');
            errores.push('horaFin');
        } else if (form.horaInicio && form.horaFin <= form.horaInicio) {
            Validators.marcarError('hor-horaFin', 'La hora de fin debe ser posterior a la de inicio');
            errores.push('horaFin');
        }

        return errores.length === 0;
    },

    // ─── EspecialidadRequest ──────────────────────────────────
    // Refleja: nombre (notBlank), descripcion (opcional)
    especialidad: (form) => {
        const errores = [];
        Validators.limpiarErrores(['esp-nombre']);

        if (Validators.estaVacio(form.nombre)) {
            Validators.marcarError('esp-nombre', 'El nombre de la especialidad es obligatorio');
            errores.push('nombre');
        }

        return errores.length === 0;
    }
};