from flask import Flask, render_template, request, redirect, url_for

# Templates live next to this file (FinalTests/*.html)
app = Flask(__name__, template_folder=".")


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


# -----------------------
# Home
# -----------------------
@app.route("/")
def home():
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
        products=view_products
    )


# -----------------------
# Product Details
# -----------------------
@app.route("/products/details")
def product_details():
    pid = request.args.get("id")
    if not pid:
        return "Missing product id"
    pid = int(pid)
    for p in products:
        if p[0] == pid:
            product = Product(
                p[0],
                p[1],
                p[2],
                p[3]
            )

            return render_template(
                "product_details.html",
                product=product
            )

    return "Invalid Product"


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
    max_id = 0
    for p in products:
        if p[0] > max_id:
            max_id = p[0]
    new_id = max_id + 1
    name = request.form.get("name")
    price = float(request.form.get("price"))
    products.append(
        [
            new_id,
            name,
            price,
            True
        ]
    )
    return redirect(
        url_for("list_products")
    )


# -----------------------
# Delete Product
# -----------------------
@app.route("/products/delete")
def delete_product():
    pid = request.args.get("id")
    if not pid:
        return "Missing product id"
    pid = int(pid)
    for p in products:
        if p[0] == pid:
            products.remove(p)
            break
    return redirect(url_for("list_products"))


# -----------------------
# Run
# -----------------------
if __name__ == "__main__":
    app.run(port=5001, debug=True)
