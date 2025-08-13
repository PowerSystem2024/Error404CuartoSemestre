from db.conexion import Conexion

SQL_CREATE_TABLE = '''
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);
'''

def crear_tabla_usuario():
    conn = Conexion.obtenerConexion()
    try:
        with conn.cursor() as cursor:
            cursor.execute(SQL_CREATE_TABLE)
            conn.commit()
            print('Tabla "usuario" creada o ya existe.')
    except Exception as e:
        print(f'Error al crear la tabla: {e}')
    finally:
        Conexion.liberarConexion(conn)

if __name__ == "__main__":
    crear_tabla_usuario()
