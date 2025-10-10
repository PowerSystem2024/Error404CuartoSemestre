# personaje.py
import pygame
import os
import colorsys
from constantes import ASSETS_PATH, SCREEN_WIDTH, SCREEN_HEIGHT

def load_img(*parts, scale=None):
    img = pygame.image.load(os.path.join(ASSETS_PATH, *parts)).convert_alpha()
    if scale:
        img = pygame.transform.scale(img, scale)
    return img

class Personaje:
    MAX_ENERGIA = 100
    SPEED = 5

    def __init__(self, x, y):
        self.image = load_img('images', 'Speeder.png', scale=(95, 95))
        self.rect = self.image.get_rect(center=(x, y))
        self.lasers = []
        self.energia = self.MAX_ENERGIA

    def mover(self, dx, dy):
        self.rect.x += dx
        self.rect.y += dy
        self.rect.clamp_ip(pygame.Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT))

    def lanzar_laser(self):
        laser = Laser(self.rect.centerx, self.rect.top)
        self.lasers.append(laser)

    def recibir_dano(self, cantidad=10):
        self.energia = max(0, self.energia - cantidad)
        return self.energia > 0

    def draw(self, screen):
        screen.blit(self.image, self.rect.topleft)

    def update_lasers(self):
        vivos = []
        for l in self.lasers:
            if l.update():
                vivos.append(l)
        self.lasers = vivos

    def draw_lasers(self, screen):
        for l in self.lasers:
            l.draw(screen)

    def energia_ratio(self):
        return self.energia / self.MAX_ENERGIA

    def _health_color(self):
        r = self.energia_ratio()
        r = r ** 0.2
        hue = (1.0/3.0) * r
        sat = 1.0
        val = 0.5
        fr, fg, fb = colorsys.hsv_to_rgb(hue, sat, val)
        return (int(fr*255), int(fg*255), int(fb*255))

    def draw_health_bar(self, screen, width=50, height=12, offset=6, position="below"):
        bar_x = self.rect.centerx - width // 2

        if position == "below":
            bar_y = self.rect.bottom + offset
        elif position == "inside":
            bar_y = self.rect.bottom - height - offset
        else:
            bar_y = self.rect.top - height - offset

        try:
            from constantes import SCREEN_HEIGHT
            if bar_y + height > SCREEN_HEIGHT:
                bar_y = self.rect.top - height - offset
        except Exception:
            pass

        # Fondo, borde y relleno
        pygame.draw.rect(screen, (40, 40, 40), (bar_x, bar_y, width, height))
        pygame.draw.rect(screen, (255, 255, 255), (bar_x, bar_y, width, height), 1)
        fill_w = int(width * self.energia_ratio())
        pygame.draw.rect(screen, self._health_color(), (bar_x, bar_y, fill_w, height))

class Enemigo:
    SPEED = 5
    def __init__(self, x, y):
        self.image = load_img('images', 'enemigo1.png', scale=(80, 80))
        self.rect = self.image.get_rect(topleft=(x, y))

    def mover(self):
        self.rect.y += self.SPEED

    def draw(self, screen):
        screen.blit(self.image, self.rect.topleft)


class Laser:
    SPEED = 10
    def __init__(self, x, y):
        self.image = load_img('images', 'laser1.png')
        self.rect = self.image.get_rect(center=(x, y))

    def update(self):
        self.rect.y -= self.SPEED
        return self.rect.bottom > 0

    def draw(self, screen):
        screen.blit(self.image, self.rect.topleft)


class Explosion:
    _FRAMES = None
    FRAME_HOLD = 20

    @classmethod
    def _get_frames(cls):
        if cls._FRAMES is None:
            cls._FRAMES = [
                load_img('images', f'regularExplosion0{i:02d}.png') for i in range(9)
            ]
        return cls._FRAMES

    def __init__(self, x, y):
        self.frames = self._get_frames()
        self.index = 0
        self.image = self.frames[self.index]
        self.rect = self.image.get_rect(center=(x, y))
        self.frame_counter = 0

    def update(self):
        self.frame_counter += 1
        if self.frame_counter >= self.FRAME_HOLD:
            self.frame_counter = 0
            self.index += 1
            if self.index >= len(self.frames):
                return False
            self.image = self.frames[self.index]
        return True

    def draw(self, screen):
        screen.blit(self.image, self.rect.topleft)
