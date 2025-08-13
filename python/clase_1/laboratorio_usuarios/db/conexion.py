from psycopg2 import pool

class Conexion:
    DATABASE = 'laboratorio_db'
    USERNAME = 'postgres'
    PASSWORD = '1234'
    DB_PORT = '5432'
    HOST = 'localhost'
    MIN = 1
    MAX = 10
    pool = None

    @classmethod
    def obtenerPool(cls):
        if cls.pool is None:
            cls.pool = pool.SimpleConnectionPool(cls.MIN, cls.MAX,
                host=cls.HOST,
                user=cls.USERNAME,
                password=cls.PASSWORD,
                database=cls.DATABASE,
                port=cls.DB_PORT)
        return cls.pool

    @classmethod
    def obtenerConexion(cls):
        return cls.obtenerPool().getconn()

    @classmethod
    def liberarConexion(cls, conexion):
        cls.obtenerPool().putconn(conexion)

    @classmethod
    def cerrarConexiones(cls):
        cls.obtenerPool().closeall()

class CursorDelPool:
    def __init__(self):
        self.conn = None
        self.cursor = None

    def __enter__(self):
        self.conn = Conexion.obtenerConexion()
        self.cursor = self.conn.cursor()
        return self.cursor

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.conn:
            self.conn.commit()
            self.cursor.close()
            Conexion.liberarConexion(self.conn)
