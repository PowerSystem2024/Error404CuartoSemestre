import psycopg2

DB_NAME = 'laboratorio_db'
USER = 'postgres'
PASSWORD = '1234'
HOST = 'localhost'
PORT = '5432'

try:
    conn = psycopg2.connect(dbname='postgres', user=USER, password=PASSWORD, host=HOST, port=PORT)
    conn.autocommit = True
    with conn.cursor() as cursor:
        cursor.execute(f"SELECT 1 FROM pg_database WHERE datname = '{DB_NAME}'")
        exists = cursor.fetchone()
        if not exists:
            cursor.execute(f"CREATE DATABASE {DB_NAME} ENCODING 'UTF8';")
            print(f"Base de datos '{DB_NAME}' creada con encoding UTF8.")
        else:
            print(f"La base de datos '{DB_NAME}' ya existe.")
except Exception as e:
    print(f"Error al crear la base de datos: {e}")
finally:
    if 'conn' in locals():
        conn.close()
