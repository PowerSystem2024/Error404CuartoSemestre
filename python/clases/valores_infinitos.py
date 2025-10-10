import math
from decimal import Decimal
# manejo de valores infinitos
infinito_positivo = float('inf')
print(f'Infinito positivo: {infinito_positivo}')
print(f'es infinito?: {math.isinf(infinito_positivo)}')

infinito_negativo = float('-inf')
print(f'Infinito negativo: {infinito_negativo}')
print(f'es infinito?: {math.isinf(infinito_negativo)}')

# modulo math
infinito_positivo = math.inf
print(f'Infinito positivo: {infinito_positivo}')
print(f'es infinito?: {math.isinf(infinito_positivo)}')

infinito_negativo = -math.inf
print(f'Infinito negativo: {infinito_negativo}')
print(f'es infinito?: {math.isinf(infinito_negativo)}')

# modulo decimal
infinito_positivo = Decimal('Infinity')
print(f'Infinito positivo (Decimal): {infinito_positivo}')
print(f'es infinito?: {infinito_positivo.is_infinite()}')

infinito_negativo = Decimal('-Infinity')
print(f'Infinito negativo (Decimal): {infinito_negativo}')
print(f'es infinito?: {infinito_negativo.is_infinite()}')