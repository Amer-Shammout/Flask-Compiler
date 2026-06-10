from flask import Flask, render_template, request, redirect, url_for

app = Flask(__name__)

name="sedra"
number=9
test_type=name+number


class Product:
    def __init__(self, pid, name, price, in_stock):
        self.id = pid
        self.name = name
        self.price = price
        self.in_stock = in_stock


# product = [id, name, price, in_stock]
products = [
    [1, "Laptop", 1200.0, True],
    [2, "Headphones", 250.0, False]
]


@app.route("/test_comp")
def comp():
    v1="sara"
    v2=9
    if v1>v2:
        print("ok")
    return 0

def no_callable():
    x=10
    funx=x()
    y="y"
    funy=y()

def test_list_error():
    arr=[1,2]
    result=[1,2]+9

def test_builtIns():
    x=10
    y="hello"
    z=[1,2,3]
    q=[1,2,"test"]
    a=abs(x)
    b=len(y)
    c=sum(z)
    a1=len(x)
    b1=sum(q)
    c1=abs(y)

def test_forloop():
    li_test=3
    for i in li_test:
        print(i)
# -----------------------
# Home
# -----------------------
@app.route("/")
def home():
    help=1>"h"
    return redirect(url_for("list_products"))


# -----------------------
# Products List
# -----------------------
@app.route("/products")
def list_products():
    view_products = []
    for p in products:
        view_products.append(
            Product(
                p[0],
                p[1],
                p[2],
                p[3]
            )
        )
    return render_template(
        "products.html",
        products=view_products,
    )


# -----------------------
# Product Details
# -----------------------
@app.route("/products/details")
def product_details():
    pid = request.args.get("id")
    if not pid:
        return "Missing product id"
    index = int(pid) - 1
    if index < 0 or index >= len(products):
        return "Invalid Product"
    p = products[index]
    product = Product(
        p[0],
        p[1],
        p[2],
        p[3]
    )
    return render_template(
        "product_details.html",
        product=product,
    )


# -----------------------
# Add Product Page
# -----------------------
@app.route("/products/add")
def add_product():
    return render_template("add_product.html")


# -----------------------
# Create Product
# -----------------------
@app.route("/products/create", methods=["GET", "POST"])
def create_product():
    if request.method == "POST":
        new_id = len(products) + 1
        name = request.form.get("name")
        price = request.form.get("price")
        price = float(price)
        new_product = [
            new_id,
            name,
            price,
            True
        ]
        products.append(new_product)
        return redirect(url_for("list_products"))
    return render_template("add_product.html")


# -----------------------
# Run
# -----------------------
if __name__ == "__main__":
    app.run(
        debug=True,
        port=8000
    )
