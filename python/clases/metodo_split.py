
# help(str.split)

cursos = 'Java Javascript Node Python Diseno'
lista_cursos = cursos.split()
print(f'Lista de cursos: {lista_cursos}')
print(type(lista_cursos))

cursos_separados_coma = 'Java, Javascript, Node, Python, Diseno'
lista_cursos = cursos_separados_coma.split(', ')
print(f'Lista de cursos: {lista_cursos}')
print(len(lista_cursos))