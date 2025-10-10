class Usuario:
    def __init__(self, id_usuario=None, username=None, password=None):
        self.id_usuario = id_usuario
        self.username = username
        self.password = password

    def __str__(self):
        return f'Usuario({self.id_usuario}, {self.username}, {self.password})'

    @property
    def id(self):
        return self.id_usuario
    @id.setter
    def id(self, value):
        self.id_usuario = value
    @property
    def user(self):
        return self.username
    @user.setter
    def user(self, value):
        self.username = value
    @property
    def pwd(self):
        return self.password
    @pwd.setter
    def pwd(self, value):
        self.password = value
