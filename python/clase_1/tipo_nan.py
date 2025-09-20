import math
from decimal import Decimal
#Nan (not a number)
a = float('nan')
print(f'a:{a}')

# Modulo math
a = float('nan')
print(f'Es de tipo Nan(not a number): {math.isnan(a)}')

# Modulo decimal
a = Decimal('nan')
print(f'Es de tipo Nan(not a number): {math.isnan(a)}')