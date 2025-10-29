import requests

def get_razas():
    r = requests.get('https://dog.ceo/api/breeds/list/all')
    print(r.status_code)
    # print(r.text)
    # print(type(r.text))  #Vemos de que tipo es el texto, puede ser un string
    #En este caso es: no es un string
    razas = r.json() 
    for raza in razas['message']: #Utilizamos la clave 'message' para acceder a las razas
        print(f" Raza de los perritos: {raza}") #Imprimimos directamente el nombre de la raza