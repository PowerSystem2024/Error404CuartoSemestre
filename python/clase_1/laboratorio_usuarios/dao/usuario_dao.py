from model.usuario import Usuario
from db.conexion import CursorDelPool

class UsuarioDAO:
    SELECCIONAR = "SELECT id_usuario, username, password FROM usuario ORDER BY id_usuario"
    INSERTAR = "INSERT INTO usuario(username, password) VALUES(%s, %s)"
    ACTUALIZAR = "UPDATE usuario SET username=%s, password=%s WHERE id_usuario=%s"
    ELIMINAR = "DELETE FROM usuario WHERE id_usuario=%s"

    @classmethod
    def seleccionar(cls):
        usuarios = []
        with CursorDelPool() as cursor:
            cursor.execute(cls.SELECCIONAR)
            registros = cursor.fetchall()
            for r in registros:
                usuario = Usuario(r[0], r[1], r[2])
                usuarios.append(usuario)
        return usuarios

    @classmethod
    def insertar(cls, usuario):
        with CursorDelPool() as cursor:
            cursor.execute(cls.INSERTAR, (usuario.username, usuario.password))

    @classmethod
    def actualizar(cls, usuario):
        with CursorDelPool() as cursor:
            cursor.execute(cls.ACTUALIZAR, (usuario.username, usuario.password, usuario.id_usuario))

    @classmethod
    def eliminar(cls, usuario):
        with CursorDelPool() as cursor:
            cursor.execute(cls.ELIMINAR, (usuario.id_usuario,))
