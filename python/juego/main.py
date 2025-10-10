import pygame
import sys
import random
import os
import math

from personaje import Personaje, Enemigo, Explosion
from constantes import SCREEN_WIDTH, SCREEN_HEIGHT, ASSETS_PATH

# ---------- Utilidades ----------
def cargar_img_fondo(nombre_archivo):
    img = pygame.image.load(os.path.join(ASSETS_PATH, 'images', nombre_archivo)).convert_alpha()
    return pygame.transform.scale(img, (SCREEN_WIDTH, SCREEN_HEIGHT))

def mostrar_imagen_inicial(screen, imagen_path, duracion_ms):
    imagen = pygame.image.load(imagen_path).convert_alpha()
    imagen = pygame.transform.scale(imagen, (SCREEN_WIDTH, SCREEN_HEIGHT))
    clock = pygame.time.Clock()
    t0 = pygame.time.get_ticks()
    while pygame.time.get_ticks() - t0 < duracion_ms:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.mixer.music.stop()
                pygame.quit()
                sys.exit()
            elif event.type == pygame.KEYDOWN and event.key == pygame.K_ESCAPE:
                pygame.mixer.music.stop()
                pygame.quit()
                sys.exit()

        keys = pygame.key.get_pressed()
        if keys[pygame.K_ESCAPE]:
            pygame.mixer.music.stop()
            pygame.quit()
            sys.exit()

        elapsed = pygame.time.get_ticks() - t0
        alpha = max(0, 255 - int(255 * (elapsed / duracion_ms)))
        imagen.set_alpha(alpha)

        screen.fill((0, 0, 0))
        screen.blit(imagen, (0, 0))
        pygame.display.flip()
        clock.tick(60)

def draw_pause_overlay(screen, font_large, font):
    # === PAUSA ===
    overlay = pygame.Surface((SCREEN_WIDTH, SCREEN_HEIGHT), pygame.SRCALPHA)
    overlay.fill((0, 0, 0, 160))
    screen.blit(overlay, (0, 0))
    t1 = font_large.render("PAUSA", True, (255, 255, 0))
    t2 = font.render("Presionar R para reanudar o ESC para salir", True, (255, 255, 255))
    x1 = SCREEN_WIDTH // 2 - t1.get_width() // 2
    y1 = SCREEN_HEIGHT // 2 - t1.get_height() // 2 - 20
    x2 = SCREEN_WIDTH // 2 - t2.get_width() // 2
    y2 = y1 + t1.get_height() + 20
    screen.blit(t1, (x1, y1))
    screen.blit(t2, (x2, y2))

# ---------- Juego ----------
def main():
    pygame.mixer.pre_init(frequency=44100, size=-16, channels=2, buffer=512)
    pygame.init()

    screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT))
    pygame.display.set_caption('Amenaza Fantasma')
    clock = pygame.time.Clock()

    try:
        icon = pygame.image.load(os.path.join(ASSETS_PATH, 'images', '001.jfif')).convert_alpha()
        pygame.display.set_icon(icon)
    except Exception:
        pass

    fondo2 = cargar_img_fondo('fondo1.jpg')
    fondo3 = cargar_img_fondo('fondo3.jpg')
    fondo_actual = fondo2

    sonido_laser = pygame.mixer.Sound(os.path.join(ASSETS_PATH, 'sounds', 'laserdis.mp3'))
    sonido_explosion = pygame.mixer.Sound(os.path.join(ASSETS_PATH, 'sounds', 'explosion.mp3'))
    pygame.mixer.music.load(os.path.join(ASSETS_PATH, 'sounds', 'efectos.mp3'))
    pygame.mixer.music.play(-1)

    imagen_inicial_path = os.path.join(ASSETS_PATH, 'images', 'inicio', 'star.png')
    if os.path.exists(imagen_inicial_path):
        mostrar_imagen_inicial(screen, imagen_inicial_path, 5000)

    font = pygame.font.Font(None, 36)
    font_large = pygame.font.Font(None, 74)

    personaje = Personaje(SCREEN_WIDTH // 2, SCREEN_HEIGHT // 2)
    enemigos = []
    explosiones = []
    puntos = 0
    nivel = 1

    siguiente_cambio_fondo = 250
    siguiente_nivel = 250

    COOLDOWN_LASER_MS = 180
    ultimo_disparo = 0

    SPEED = 5

    # === PAUSA === estado
    paused = False

    running = True
    while running:
        dt = clock.tick(60) / 1000.0

        # --- Eventos ---
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False

            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    running = False
                # === PAUSA === toggle y reanudar
                elif event.key == pygame.K_p and not paused:
                    paused = True
                    try:
                        pygame.mixer.music.pause()
                    except Exception:
                        pass
                elif event.key == pygame.K_r and paused:
                    paused = False
                    try:
                        pygame.mixer.music.unpause()
                    except Exception:
                        pass

        keys = pygame.key.get_pressed()
        if keys[pygame.K_ESCAPE]:
            running = False

        # === PAUSA ===
        if paused:
            screen.blit(fondo_actual, (0, 0))
            personaje.draw_health_bar(screen, width=80, height=12, offset=6, position="below")
            personaje.draw(screen)
            personaje.draw_lasers(screen)
            for enemigo in enemigos:
                enemigo.draw(screen)
            for ex in explosiones:
                ex.draw(screen)
            # HUD (estático en pausa)
            texto_puntos = font.render(f"Puntos: {puntos}", True, (255, 255, 255))
            texto_nivel = font.render(f"Nivel: {nivel}", True, (255, 255, 255))
            screen.blit(texto_puntos, (10, 50))
            screen.blit(texto_nivel, (10, 90))
            # pygame.draw.rect(screen, (255, 0, 0), (10, 10, 100, 10))
            # pygame.draw.rect(screen, (0, 255, 0), (10, 10, int(100 * personaje.energia_ratio()), 10))

            draw_pause_overlay(screen, font_large, font)
            pygame.display.flip()
            continue  # <- salta actualizaciones mientras está en pausa

        # --- Input movimiento ---
        dx = (keys[pygame.K_RIGHT] - keys[pygame.K_LEFT])
        dy = (keys[pygame.K_DOWN] - keys[pygame.K_UP])
        if dx != 0 or dy != 0:
            mag = math.sqrt(dx*dx + dy*dy)
            dx = dx / mag * SPEED
            dy = dy / mag * SPEED
        personaje.mover(dx, dy)

        # Disparo con cooldown
        if keys[pygame.K_SPACE]:
            ahora = pygame.time.get_ticks()
            if ahora - ultimo_disparo >= COOLDOWN_LASER_MS:
                personaje.lanzar_laser()
                try:
                    sonido_laser.play()
                except Exception:
                    pass
                ultimo_disparo = ahora

        # Spawn de enemigos (escalado por dt y nivel)
        spawn_por_seg = 1.2 + 0.35 * (nivel - 1)
        if random.random() < spawn_por_seg * dt:
            x = random.randint(0, SCREEN_WIDTH - 50)
            enemigos.append(Enemigo(x, -60))

        # Update / limpiar
        enemigos_vivos = []
        for enemigo in enemigos:
            enemigo.mover()
            if enemigo.rect.top <= SCREEN_HEIGHT:
                enemigos_vivos.append(enemigo)
        enemigos = enemigos_vivos

        personaje.update_lasers()

        # Colisiones láser-enemigo
        for enemigo in enemigos[:]:
            for laser in personaje.lasers[:]:
                if enemigo.rect.colliderect(laser.rect):
                    explosiones.append(Explosion(enemigo.rect.centerx, enemigo.rect.centery))
                    try:
                        sonido_explosion.play()
                    except Exception:
                        pass
                    enemigos.remove(enemigo)
                    personaje.lasers.remove(laser)
                    puntos += 10
                    break

        # Colisión enemigo-personaje
        for enemigo in enemigos[:]:
            if enemigo.rect.colliderect(personaje.rect):
                if not personaje.recibir_dano():
                    running = False
                enemigos.remove(enemigo)
                explosiones.append(Explosion(enemigo.rect.centerx, enemigo.rect.centery))
                try:
                    sonido_explosion.play()
                except Exception:
                    pass

        # Explosiones
        explosiones = [e for e in explosiones if e.update()]

        # Umbrales por puntos
        if puntos >= siguiente_cambio_fondo:
            fondo_actual = fondo3 if fondo_actual == fondo2 else fondo2
            siguiente_cambio_fondo += 250

        if puntos >= siguiente_nivel:
            nivel += 1
            siguiente_nivel += 250

        # --- Render ---
        screen.blit(fondo_actual, (0, 0))
        personaje.draw_health_bar(screen, width=80, height=12, offset=6, position="below")
        personaje.draw(screen)
        personaje.draw_lasers(screen)
        for enemigo in enemigos:
            enemigo.draw(screen)
        for ex in explosiones:
            ex.draw(screen)

        texto_puntos = font.render(f"Puntos: {puntos}", True, (255, 255, 255))
        texto_nivel = font.render(f"Nivel: {nivel}", True, (255, 255, 255))
        screen.blit(texto_puntos, (10, 50))
        screen.blit(texto_nivel, (10, 90))
        # pygame.draw.rect(screen, (255, 0, 0), (10, 10, 100, 10))
        # pygame.draw.rect(screen, (0, 255, 0), (10, 10, int(100 * personaje.energia_ratio()), 10))

        pygame.display.flip()

    # ---------- GAME OVER ----------
    screen.fill((0, 0, 0))
    texto_game_over = font_large.render("GAME OVER", True, (255, 0, 0))
    texto_mensaje = font.render("Que la Fuerza te acompañe", True, (255, 255, 255))

    pos_x_go = SCREEN_WIDTH // 2 - texto_game_over.get_width() // 2
    pos_y_go = SCREEN_HEIGHT // 2 - texto_game_over.get_height() // 2 - 20
    pos_x_msg = SCREEN_WIDTH // 2 - texto_mensaje.get_width() // 2
    pos_y_msg = SCREEN_HEIGHT // 2 + texto_game_over.get_height() // 2 + 20

    screen.blit(texto_game_over, (pos_x_go, pos_y_go))
    screen.blit(texto_mensaje, (pos_x_msg, pos_y_msg))
    pygame.display.flip()
    pygame.time.wait(2000)

    pygame.mixer.music.stop()
    pygame.quit()
    sys.exit()


if __name__ == '__main__':
    main()
