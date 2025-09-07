from dao.usuario_dao import UsuarioDAO
from model.usuario import Usuario

class MenuAppUsuario:
    @staticmethod
    def mostrar_menu():
        while True:
            print("\n" + "="*40)
            print("      LABORATORIO USUARIOS")
            print("="*40)
            print("[1] Listar usuarios")
            print("[2] Agregar usuario")
            print("[3] Actualizar usuario")
            print("[4] Eliminar usuario")
            print("[5] Salir")
            print("="*40)
            opcion = input("\nSeleccione una opción [1-5]: ")
            print()
            if opcion == '1':
                while True:
                    print("\n" + "="*40)
                    print("      LISTA DE USUARIOS")
                    print("="*40)
                    usuarios = UsuarioDAO.seleccionar()
                    if usuarios:
                        print(f"{'ID':<5} {'Username':<20}")
                        print("-"*30)
                        for u in usuarios:
                            print(f"{u.id_usuario:<5} {u.username:<20}")
                    else:
                        print("No hay usuarios registrados.")
                    input("\nPresione cualquier tecla para volver atrás...")
                    break
            elif opcion == '2':
                while True:
                    print("\n" + "="*40)
                    print("      AGREGAR USUARIO")
                    print("="*40)
                    username = input("Ingrese el nombre de usuario: ").strip()
                    password = input("Ingrese la contraseña: ").strip()
                    if not username or not password:
                        print("\nError: El nombre de usuario y la contraseña no pueden estar vacíos.")
                        print("[1] Corregir datos")
                        print("[2] Volver al menú principal")
                        subopcion = input("Seleccione una opción [1-2]: ")
                        if subopcion == '1':
                            continue
                        elif subopcion == '2':
                            break
                        else:
                            print("Opción inválida. Volviendo al menú principal...")
                            break
                    try:
                        usuario = Usuario(username=username, password=password)
                        UsuarioDAO.insertar(usuario)
                        print("\nUsuario agregado correctamente.")
                    except Exception as e:
                        print(f"\nError al agregar usuario: {e}")
                    input("\nPresione cualquier tecla para volver atrás...")
                    break
            elif opcion == '3':
                while True:
                    print("\n" + "="*40)
                    print("      ACTUALIZAR USUARIO")
                    print("="*40)
                    print("¿Cómo desea buscar el usuario?")
                    print("[1] Por ID")
                    print("[2] Por Username")
                    print("[3] Volver al menú principal")
                    modo = input("Seleccione una opción [1-3]: ")
                    usuarios = UsuarioDAO.seleccionar()
                    usuario_encontrado = None
                    if modo == '1':
                        id_usuario = input("Ingrese el ID del usuario a actualizar: ").strip()
                        if not id_usuario.isdigit():
                            print("ID inválido. Debe ser un número.")
                            continue
                        for u in usuarios:
                            if u.id_usuario == int(id_usuario):
                                usuario_encontrado = u
                                break
                        if not usuario_encontrado:
                            print(f"\nError: No se encontró el usuario con ID {id_usuario}.")
                            print("[1] Corregir ID")
                            print("[2] Volver al menú principal")
                            subopcion = input("Seleccione una opción [1-2]: ")
                            if subopcion == '1':
                                continue
                            elif subopcion == '2':
                                break
                            else:
                                print("Opción inválida. Volviendo al menú principal...")
                                break
                    elif modo == '2':
                        username = input("Ingrese el username del usuario a actualizar: ").strip()
                        if not username:
                            print("Username no puede estar vacío.")
                            continue
                        encontrados = [u for u in usuarios if username.lower() in u.username.lower()]
                        if not encontrados:
                            print(f"\nError: No se encontró el usuario con username '{username}'.")
                            print("[1] Corregir username")
                            print("[2] Volver al menú principal")
                            subopcion = input("Seleccione una opción [1-2]: ")
                            if subopcion == '1':
                                continue
                            elif subopcion == '2':
                                break
                            else:
                                print("Opción inválida. Volviendo al menú principal...")
                                break
                        if len(encontrados) > 1:
                            print("\nSe encontraron varios usuarios con ese username:")
                            print(f"{'ID':<5} {'Username':<20}")
                            print("-"*30)
                            for u in encontrados:
                                print(f"{u.id_usuario:<5} {u.username:<20}")
                            print("[C] Cancelar y volver al menú principal")
                            id_usuario = input("\nIngrese el ID del usuario que desea actualizar o 'C' para cancelar: ").strip()
                            if id_usuario.lower() == 'c':
                                break
                            usuario_encontrado = None
                            for u in encontrados:
                                if str(u.id_usuario) == id_usuario:
                                    usuario_encontrado = u
                                    break
                            if not usuario_encontrado:
                                print("ID inválido. Volviendo al menú principal...")
                                break
                        else:
                            usuario_encontrado = encontrados[0]
                    elif modo == '3':
                        break
                    else:
                        print("Opción inválida. Volviendo al menú principal...")
                        break
                    print("\n¿Qué campo desea actualizar?")
                    print("[1] Username")
                    print("[2] Password")
                    print("[3] Ambos")
                    campo = input("Seleccione una opción [1-3]: ")
                    nuevo_username = usuario_encontrado.username
                    nuevo_password = usuario_encontrado.password
                    if campo == '1':
                        nuevo_username = input("Nuevo nombre de usuario: ").strip()
                        if not nuevo_username:
                            print("\nError: El nombre de usuario no puede estar vacío.")
                            continue
                    elif campo == '2':
                        nuevo_password = input("Nueva contraseña: ").strip()
                        if not nuevo_password:
                            print("\nError: La contraseña no puede estar vacía.")
                            continue
                    elif campo == '3':
                        nuevo_username = input("Nuevo nombre de usuario: ").strip()
                        nuevo_password = input("Nueva contraseña: ").strip()
                        if not nuevo_username or not nuevo_password:
                            print("\nError: El nombre de usuario y la contraseña no pueden estar vacíos.")
                            continue
                    else:
                        print("Opción inválida. Volviendo al menú principal...")
                        break
                    try:
                        usuario = Usuario(usuario_encontrado.id_usuario, nuevo_username, nuevo_password)
                        UsuarioDAO.actualizar(usuario)
                        print("\nUsuario actualizado correctamente.")
                    except Exception as e:
                        print(f"\nError al actualizar usuario: {e}")
                    input("\nPresione cualquier tecla para volver atrás...")
                    break
            elif opcion == '4':
                while True:
                    print("\n" + "="*40)
                    print("      ELIMINAR USUARIO")
                    print("="*40)
                    print("¿Cómo desea eliminar el usuario?")
                    print("[1] Por ID")
                    print("[2] Por Username")
                    print("[3] Volver al menú principal")
                    modo = input("Seleccione una opción [1-3]: ")
                    if modo == '1':
                        id_usuario = input("Ingrese el ID del usuario a eliminar: ").strip()
                        if not id_usuario.isdigit():
                            print("ID inválido. Debe ser un número.")
                            continue
                        usuarios = UsuarioDAO.seleccionar()
                        encontrado = False
                        for u in usuarios:
                            if u.id_usuario == int(id_usuario):
                                encontrado = True
                                break
                        if not encontrado:
                            print(f"\nError: No se encontró el usuario con ID {id_usuario}.")
                            print("[1] Corregir ID")
                            print("[2] Volver al menú principal")
                            subopcion = input("Seleccione una opción [1-2]: ")
                            if subopcion == '1':
                                continue
                            elif subopcion == '2':
                                break
                            else:
                                print("Opción inválida. Volviendo al menú principal...")
                                break
                        try:
                            usuario = Usuario(id_usuario=int(id_usuario))
                            UsuarioDAO.eliminar(usuario)
                            print("\nUsuario eliminado correctamente.")
                        except Exception as e:
                            print(f"\nError al eliminar usuario: {e}")
                        input("\nPresione cualquier tecla para volver atrás...")
                        break
                    elif modo == '2':
                        username = input("Ingrese el username del usuario a eliminar: ").strip()
                        if not username:
                            print("Username no puede estar vacío.")
                            continue
                        usuarios = UsuarioDAO.seleccionar()
                        encontrados = [u for u in usuarios if username.lower() in u.username.lower()]
                        if not encontrados:
                            print(f"\nError: No se encontró el usuario con username '{username}'.")
                            print("[1] Corregir username")
                            print("[2] Volver al menú principal")
                            subopcion = input("Seleccione una opción [1-2]: ")
                            if subopcion == '1':
                                continue
                            elif subopcion == '2':
                                break
                            else:
                                print("Opción inválida. Volviendo al menú principal...")
                                break
                        if len(encontrados) > 1:
                            print("\nSe encontraron varios usuarios con ese username:")
                            print(f"{'ID':<5} {'Username':<20}")
                            print("-"*30)
                            for u in encontrados:
                                print(f"{u.id_usuario:<5} {u.username:<20}")
                            print("[C] Cancelar y volver al menú principal")
                            id_usuario = input("\nIngrese el ID del usuario que desea eliminar o 'C' para cancelar: ").strip()
                            if id_usuario.lower() == 'c':
                                break
                            usuario_encontrado = None
                            for u in encontrados:
                                if str(u.id_usuario) == id_usuario:
                                    usuario_encontrado = u
                                    break
                            if not usuario_encontrado:
                                print("ID inválido. Volviendo al menú principal...")
                                break
                        else:
                            usuario_encontrado = encontrados[0]
                        try:
                            usuario = Usuario(id_usuario=usuario_encontrado.id_usuario)
                            UsuarioDAO.eliminar(usuario)
                            print("\nUsuario eliminado correctamente.")
                        except Exception as e:
                            print(f"\nError al eliminar usuario: {e}")
                        input("\nPresione cualquier tecla para volver atrás...")
                        break
                    elif modo == '3':
                        break
                    else:
                        print("Opción inválida. Volviendo al menú principal...")
                        break
            elif opcion == '5':
                print("\n¡Gracias por usar el sistema!")
                print("Saliendo...")
                break
            else:
                print("Opción inválida. Por favor, ingrese un número del 1 al 5.")
