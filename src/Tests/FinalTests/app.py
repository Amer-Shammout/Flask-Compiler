from flask import Flask, render_template, request, redirect, url_for

app = Flask("app")

# product = [id, name, price, in_stock]
products = [[1, "Laptop", 1200.0, True],[2, "Headphones", 250.0, False]]

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
    return render_template("products.html", products)

# -----------------------
# Product Details
# -----------------------
@app.route("/products/details")
def product_details():
    pid = request.args.get("id")
    
    if pid == None:
        return "Missing product id"
    
    index = int(pid) - 1
    
    if index < 0:
        return "Invalid Product"
    
    return render_template("product_details.html",products[index])

# -----------------------
# Add Product Page
# -----------------------
@app.route("/products/add")
def add_product():
    return render_template("add_product.html")

# -----------------------
# Create Product
# -----------------------
@app.route("/products/create")
def create_product():
    new_id = len(products) + 1
    new_product = [new_id, "Mouse", 50.0, True]
    products.append(new_product)
    
    return redirect(url_for("list_products"))
