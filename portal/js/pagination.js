/**
 * pagination.js — Paginación reutilizable en el navegador (sin backend).
 *
 * Uso:
 *   const pager = new Paginador({
 *     contenedorId: 'pager-pagos',   // <div id="pager-pagos"></div> debajo de la tabla
 *     porPagina: 10,
 *     onRenderPagina: (itemsPagina) => renderTabla(itemsPagina)
 *   });
 *   pager.setDatos(todosPagos);      // llamar cada vez que cambian los datos o filtros
 *
 * @author Equipo Curso Integrador UTP 2026
 */
class Paginador {
    constructor({ contenedorId, porPagina = 10, onRenderPagina }) {
        this.contenedorId = contenedorId;
        this.porPagina = porPagina;
        this.onRenderPagina = onRenderPagina;
        this.datos = [];
        this.paginaActual = 1;

        // Expone esta instancia para que los botones (onclick inline) puedan llamarla
        window[`${this.contenedorId}__paginador`] = this;
    }

    /** Reemplaza el dataset completo (por ejemplo tras cargar o filtrar) y vuelve a la página 1. */
    setDatos(datos) {
        this.datos = datos || [];
        this.paginaActual = 1;
        this._render();
    }

    /** Navega a una página específica (se ajusta automáticamente a los límites válidos). */
    irAPagina(n) {
        const totalPaginas = this._totalPaginas();
        this.paginaActual = Math.min(Math.max(1, n), totalPaginas || 1);
        this._render();
    }

    _totalPaginas() {
        return Math.max(1, Math.ceil(this.datos.length / this.porPagina));
    }

    _render() {
        const total = this.datos.length;
        const totalPaginas = this._totalPaginas();
        const inicio = (this.paginaActual - 1) * this.porPagina;
        const fin = Math.min(inicio + this.porPagina, total);
        const itemsPagina = this.datos.slice(inicio, fin);

        this.onRenderPagina(itemsPagina);
        this._renderControles(total, totalPaginas, inicio, fin);
    }

    _renderControles(total, totalPaginas, inicio, fin) {
        const cont = document.getElementById(this.contenedorId);
        if (!cont) return;

        if (total === 0) {
            cont.innerHTML = '';
            return;
        }

        const rango = `${inicio + 1}–${fin} de ${total}`;

        // Ventana de hasta 5 números de página alrededor de la actual
        const totalBotones = Math.min(5, totalPaginas);
        let desde = Math.max(1, this.paginaActual - 2);
        let hasta = Math.min(totalPaginas, desde + totalBotones - 1);
        desde = Math.max(1, hasta - totalBotones + 1);

        const paginas = [];
        for (let i = desde; i <= hasta; i++) paginas.push(i);

        const botonPagina = (n) => `
      <button onclick="window['${this.contenedorId}__paginador'].irAPagina(${n})"
        class="min-w-[2rem] h-8 px-2 rounded-lg text-xs font-medium transition-colors ${n === this.paginaActual
                ? 'bg-rumbo text-white'
                : 'bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white'
            }">${n}</button>`;

        cont.innerHTML = `
      <div class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-borde dark:border-borde-dark">
        <span class="text-xs text-neblina">Mostrando ${rango}</span>
        <div class="flex items-center gap-1">
          <button onclick="window['${this.contenedorId}__paginador'].irAPagina(${this.paginaActual - 1})"
            ${this.paginaActual === 1 ? 'disabled' : ''}
            class="w-8 h-8 rounded-lg text-xs bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina disabled:opacity-40 disabled:cursor-not-allowed hover:text-tinta dark:hover:text-white transition-colors">
            <i class="bi bi-chevron-left"></i>
          </button>
          ${paginas.map(botonPagina).join('')}
          <button onclick="window['${this.contenedorId}__paginador'].irAPagina(${this.paginaActual + 1})"
            ${this.paginaActual === totalPaginas ? 'disabled' : ''}
            class="w-8 h-8 rounded-lg text-xs bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina disabled:opacity-40 disabled:cursor-not-allowed hover:text-tinta dark:hover:text-white transition-colors">
            <i class="bi bi-chevron-right"></i>
          </button>
        </div>
      </div>`;
    }
}