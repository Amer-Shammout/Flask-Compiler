"""
Total expected Flask errors: 9

- E102: str + int
- E103: str > int
- E104: int not callable (x())
- E104: str not callable (y())
- E105: list + int
- E106: len(int)
- E106: sum(list with str)
# E106: bad operand type for abs(): 'str'
- E107: for loop over int
"""
from flask import Flask, render_template

app = Flask(__name__)

name = "sedra"
number = 9
test_type = name + number      # E102: TypeError str + int

items = [1, 2, 3]              # Defined in Flask global, but not passed later
age = 25                       # Defined in Flask global, but not passed later


class Product:
    def __init__(self, pid, name, price, in_stock):
        self.id = pid
        self.name = name
        self.price = price
        self.in_stock = in_stock


@app.route("/test_comp")
def comp():
    v1 = "sara"
    v2 = 9
    if v1 > v2:                 # E103: TypeError '>' not supported between str and int
        print("ok")
    return 0


def no_callable():
    x = 10
    funx = x()                  # E104: 'int' object is not callable

    y = "y"
    funy = y()                  # E104: 'str' object is not callable


def test_list_error():
    result = [1, 2] + 9         # E105: can only concatenate list (not "int") to list


def test_builtins():
    x = 10
    y = "hello"
    z = [1, 2, 3]
    q = [1, 2, "test"]

    a = abs(x)                  # OK
    b = len(y)                  # OK
    c = sum(z)                  # OK

    a1 = len(x)                 # E106: object of type 'int' has no len()
    b1 = sum(q)                 # E106: unsupported operand type 'str' in sum()
    c1 = abs(y)                 # E106: bad operand type for abs(): 'str'


def test_forloop():
    li_test = 3
    for i in li_test:           # E107: 'int' object is not iterable
        print(i)


@app.route("/")
def home():
    # user_name passed, but age and items are not passed
    return render_template("example.html", user_name="Sedra")