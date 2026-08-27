from flask import Flask, render_template, request, redirect, url_for

app = Flask(__name__)

name = "sedra"
number = 9
test_type = name + number          # E102: str + int

class Product:
    def __init__(self, pid, name, price, in_stock):
        self.id = pid
        self.name = name
        self.price = price
        self.in_stock = in_stock

products = [
    [1, "Laptop", 1200.0, True],
    [2, "Headphones", 250.0, False]
]

@app.route("/test_comp")
def comp():
    v1 = "sara"
    v2 = 9
    if v1 > v2:                    # E103: str > int
        print("ok")
    return 0

def no_callable():
    x = 10
    funx = x()                     # E104: int not callable
    y = "y"
    funy = y()                     # E104: str not callable

def test_list_error():
    arr = [1, 2]
    result = [1, 2] + 9            # E105: list + int

def test_builtins():
    x = 10
    y = "hello"
    z = [1, 2, 3]
    q = [1, 2, "test"]

    a = abs(x)                     # OK
    b = len(y)                     # OK
    c = sum(z)                     # OK (list of int)
    a1 = len(x)                    # E106: len(int)
    b1 = sum(q)                    # E106: sum(list with str)
    c1 = abs(y)                    # E106: abs(str)

def test_forloop():
    li_test = 3
    for i in li_test:              # E107: int not iterable
        print(i)

# ---------- NEW CASES FROM RECENT FIXES ----------

def test_elif():
    if 5 > 3:
        x = 1
    elif "a" > 3:                  # E103: str > int (was missed before)
        x = 2
    else:
        x = 3
    return x

def test_keyword_arg():
    # E102 inside keyword argument should now be caught
    process(amount="5" + 1)        # E102: str + int

def test_bitwise_not():
    y = "text"
    z = ~y                          # E102: bad operand type for unary ~ (str)

# Helper for keyword test (not defined; just for demonstration)
def process(amount):
    pass

# -----------------------
# Home
# -----------------------
@app.route("/")
def home():
    help = 1 > "h"                 # E103: int > str
    return redirect(url_for("list_products"))

@app.route("/products")
def list_products():
    view_products = []
    for p in products:
        view_products.append(
            Product(p[0], p[1], p[2], p[3])
        )
    return render_template("products.html", products=view_products)

@app.route("/products/details")
def product_details():
    pid = request.args.get("id")
    if not pid:
        return "Missing product id"
    index = int(pid) - 1
    if index < 0 or index >= len(products):
        return "Invalid Product"
    p = products[index]
    product = Product(p[0], p[1], p[2], p[3])
    return render_template("product_details.html", product=product)

@app.route("/products/add")
def add_product():
    return render_template("add_product.html")

@app.route("/products/create", methods=["GET", "POST"])
def create_product():
    if request.method == "POST":
        new_id = len(products) + 1
        name = request.form.get("name")
        price = request.form.get("price")
        price = float(price)
        new_product = [new_id, name, price, True]
        products.append(new_product)
        return redirect(url_for("list_products"))
    return render_template("add_product.html")

if __name__ == "__main__":
    app.run(debug=True, port=8000)
