// =============================
// VARIABLES GLOBALES DEL JUEGO
// =============================
const personajes = ["Zuko", "Katara", "Aang", "Toph"];
const ataques = ["Puño", "Patada", "Barrida"];

// Objeto global DRY para acceso al DOM
const dom = {
    btnPersonaje: () => document.getElementById('boton-personaje'),
    btnPunio: () => document.getElementById('boton-punio'),
    btnPatada: () => document.getElementById('boton-patada'),
    btnBarrida: () => document.getElementById('boton-barrida'),
    btnReglas: () => document.getElementById('boton-reglas'),
    btnCerrarReglas: () => document.getElementById('cerrar-reglas'),
    btnReiniciar: () => document.getElementById('btn-reiniciar'),
    seleccionarAtaque: () => document.getElementById('seleccionar-ataque'),
    seleccionarPersonaje: () => document.getElementById('seleccionar-personaje'),
    modalReglas: () => document.getElementById('modal-reglas'),
    spanPersonajeJugador: () => document.getElementById('personaje-jugador'),
    spanPersonajeEnemigo: () => document.getElementById('personaje-enemigo'),
    spanVidasJugador: () => document.getElementById('vidas-jugador'),
    spanVidasEnemigo: () => document.getElementById('vidas-enemigo'),
    mensajes: () => document.getElementById('mensajes'),
    checks: () => personajes.map(p => document.getElementById(p.toLowerCase()))
};

// =============================
// CLASES (POO)
// =============================

class Jugador {
    constructor(nombre) {
        this.nombre = nombre;
        this.vidas = 3;
        this.ataque = null;
    }

    seleccionarAtaque(tipo) {
        this.ataque = tipo;
    }

    perderVida() {
        this.vidas = Math.max(this.vidas - 1, 0);
    }

    reiniciar() {
        this.vidas = 3;
        this.ataque = null;
    }

    estaVivo() {
        return this.vidas > 0;
    }
}

class Juego {
    constructor() {
        this.jugador = null;
        this.enemigo = null;
        this.juegoTerminado = false;
    }

    iniciar() {
        this.configurarEventos();
        this.ocultarSeccionesIniciales();
    }

    configurarEventos() {
        // Eventos principales
        dom.btnPersonaje().addEventListener('click', () => this.seleccionarPersonajeJugador());
        dom.btnPunio().addEventListener('click', () => this.seleccionarAtaque('Puño'));
        dom.btnPatada().addEventListener('click', () => this.seleccionarAtaque('Patada'));
        dom.btnBarrida().addEventListener('click', () => this.seleccionarAtaque('Barrida'));
        dom.btnReiniciar().addEventListener('click', () => this.reiniciarJuego());
        
        // Eventos del modal
        dom.btnReglas().addEventListener('click', mostrarReglas);
        dom.btnCerrarReglas().addEventListener('click', cerrarReglas);
        window.addEventListener('click', (event) => {
            if (event.target === dom.modalReglas()) cerrarReglas();
        });
    }

    ocultarSeccionesIniciales() {
        dom.seleccionarAtaque().style.display = 'none';
        dom.btnReiniciar().style.display = 'none'; // ← Botón inicialmente oculto
    }

    seleccionarPersonajeJugador() {
        if (this.jugador && this.enemigo) return; // Evitar reselección
        
        const seleccionado = personajes.find(p => 
            document.getElementById(p.toLowerCase()).checked
        );
        
        if (!seleccionado) {
            alert("Por favor, selecciona un personaje");
            return;
        }

        this.jugador = new Jugador(seleccionado);
        dom.spanPersonajeJugador().innerHTML = seleccionado;
        alert("Has seleccionado a " + seleccionado); // ← Alert agregado
        
        this.seleccionarPersonajeEnemigo();
        this.mostrarSeccionAtaque();
    }

    seleccionarPersonajeEnemigo() {
        const enemigoNombre = personajes[Math.floor(Math.random() * personajes.length)];
        this.enemigo = new Jugador(enemigoNombre);
        dom.spanPersonajeEnemigo().innerHTML = enemigoNombre;
        alert("El enemigo ha seleccionado a " + enemigoNombre); // ← Alert agregado
    }

    mostrarSeccionAtaque() {
        dom.seleccionarAtaque().style.display = 'block';
        dom.seleccionarPersonaje().style.display = 'none';
    }

    seleccionarAtaque(tipoAtaque) {
        if (this.juegoTerminado) return; // No permitir ataques si el juego terminó
        
        this.jugador.seleccionarAtaque(tipoAtaque);
        
        // Ataque aleatorio del enemigo
        const ataqueEnemigo = ataques[Math.floor(Math.random() * ataques.length)];
        this.enemigo.seleccionarAtaque(ataqueEnemigo);
        
        this.combate();
    }

    combate() {
        let resultado = this.determinarResultado();
        
        // Animación visual
        this.animarPantalla();
        
        // Aplicar daño según resultado
        this.aplicarDano(resultado);
        
        // Mostrar mensaje del combate
        this.crearMensaje(resultado);
        
        // Verificar si el juego terminó
        this.verificarFinDelJuego();
    }

    determinarResultado() {
        const ataqueJugador = this.jugador.ataque;
        const ataqueEnemigo = this.enemigo.ataque;

        if (ataqueJugador === ataqueEnemigo) {
            return "EMPATE";
        } else if (
            (ataqueJugador === 'Puño' && ataqueEnemigo === 'Barrida') ||
            (ataqueJugador === 'Patada' && ataqueEnemigo === 'Puño') ||
            (ataqueJugador === 'Barrida' && ataqueEnemigo === 'Patada')
        ) {
            return 'GANASTE';
        } else {
            return 'PERDISTE';
        }
    }

    aplicarDano(resultado) {
        if (resultado === 'GANASTE') {
            this.enemigo.perderVida();
            dom.spanVidasEnemigo().innerHTML = this.enemigo.vidas;
        } else if (resultado === 'PERDISTE') {
            this.jugador.perderVida();
            dom.spanVidasJugador().innerHTML = this.jugador.vidas;
        }
    }

    animarPantalla() {
        const body = document.body;
        body.classList.add('anim-ataque');
        setTimeout(() => body.classList.remove('anim-ataque'), 400);
    }

    crearMensaje(resultado) {
        const parrafo = document.createElement('p');
        parrafo.innerHTML = `Tu personaje atacó con ${this.jugador.ataque}, el personaje enemigo atacó con ${this.enemigo.ataque} → <strong>${resultado}</strong>`;
        
        // Agregar clase CSS según el resultado
        parrafo.classList.add(resultado === 'GANASTE' ? 'mensaje-victoria' : 
                            resultado === 'PERDISTE' ? 'mensaje-derrota' : 'mensaje-empate');
        
        dom.mensajes().appendChild(parrafo);
        
        // Scroll automático al último mensaje
        dom.mensajes().scrollTop = dom.mensajes().scrollHeight;
    }

    crearMensajeFinal(mensaje) {
        const parrafo = document.createElement('p');
        parrafo.innerHTML = mensaje;
        parrafo.style.fontSize = '1.5rem';
        parrafo.style.textAlign = 'center';
        parrafo.style.marginTop = '20px';
        parrafo.style.fontWeight = 'bold';
        
        dom.mensajes().appendChild(parrafo);
        dom.mensajes().scrollTop = dom.mensajes().scrollHeight;
    }

    verificarFinDelJuego() {
        if (!this.enemigo.estaVivo()) {
            this.finalizarJuego("🎉 ¡FELICITACIONES! ¡HAS GANADO! 🎉");
        } else if (!this.jugador.estaVivo()) {
            this.finalizarJuego("💀 GAME OVER - ¡HAS SIDO DERROTADO! 💀");
        }
    }

    finalizarJuego(mensaje) {
        this.juegoTerminado = true;
        this.crearMensajeFinal(mensaje);
        this.deshabilitarBotonesAtaque();
        
        // Alert según el resultado
        if (mensaje.includes("GANADO")) {
            alert("¡Has ganado la partida!");
        } else if (mensaje.includes("DERROTADO")) {
            alert("Has perdido la partida...");
        }
        
        this.mostrarBotonReiniciar(); // ← Mostrar botón solo al terminar
    }

    deshabilitarBotonesAtaque() {
        const botones = [dom.btnPunio(), dom.btnPatada(), dom.btnBarrida()];
        botones.forEach(boton => {
            boton.disabled = true;
            boton.style.opacity = '0.5';
            boton.style.cursor = 'not-allowed';
        });
    }

    habilitarBotonesAtaque() {
        const botones = [dom.btnPunio(), dom.btnPatada(), dom.btnBarrida()];
        botones.forEach(boton => {
            boton.disabled = false;
            boton.style.opacity = '1';
            boton.style.cursor = 'pointer';
        });
    }

    mostrarBotonReiniciar() {
        dom.btnReiniciar().style.display = 'block'; // ← Solo aquí se muestra
    }

    ocultarBotonReiniciar() {
        dom.btnReiniciar().style.display = 'none'; // ← Se oculta al reiniciar
    }

    reiniciarJuego() {
        // Reiniciar estado del juego
        this.jugador = null;
        this.enemigo = null;
        this.juegoTerminado = false;
        
        // Limpiar interfaz
        dom.mensajes().innerHTML = "";
        dom.spanPersonajeJugador().innerHTML = "";
        dom.spanPersonajeEnemigo().innerHTML = "";
        dom.spanVidasJugador().innerHTML = "3";
        dom.spanVidasEnemigo().innerHTML = "3";
        
        // Deseleccionar personajes
        dom.checks().forEach(check => check.checked = false);
        
        // Habilitar botones y mostrar secciones correctas
        this.habilitarBotonesAtaque();
        dom.seleccionarPersonaje().style.display = 'block';
        dom.seleccionarAtaque().style.display = 'none';
        this.ocultarBotonReiniciar(); // ← Ocultar botón al reiniciar
    }
}

// =============================
// FUNCIONES GLOBALES AUXILIARES
// =============================

function mostrarReglas() {
    dom.modalReglas().style.display = 'flex';
}

function cerrarReglas() {
    dom.modalReglas().style.display = 'none';
}

// Mejoras para UI en pantallas pequeñas
function scrollYResaltaMensajes() {
    const mensajesSection = dom.mensajes();
    const observer = new MutationObserver(() => {
        if (mensajesSection.lastElementChild) {
            mensajesSection.lastElementChild.classList.add('mensaje-ultimo');
            // Quitar resaltado de mensajes anteriores
            let prev = mensajesSection.lastElementChild.previousElementSibling;
            while (prev) {
                prev.classList.remove('mensaje-ultimo');
                prev = prev.previousElementSibling;
            }
            mensajesSection.scrollTop = mensajesSection.scrollHeight;
        }
    });
    observer.observe(mensajesSection, { childList: true });
}

// =============================
// INICIALIZACIÓN DEL JUEGO
// =============================

// Variable global para la instancia del juego
let juegoAvatar;

window.addEventListener('DOMContentLoaded', () => {
    // Crear instancia global del juego
    juegoAvatar = new Juego();
    juegoAvatar.iniciar();
    
    // Inicializar mejoras de UI
    scrollYResaltaMensajes();
});