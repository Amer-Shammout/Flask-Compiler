"""
End-to-end check for FinalTests Flask app (screens + navigation).

Run from project root:
  python src/Tests/FinalTests/e2e_check.py

Or from FinalTests:
  python e2e_check.py
"""

from __future__ import annotations

import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)

# Import after path setup
import app as flask_app  # noqa: E402


def main() -> None:
    client = flask_app.app.test_client()
    failures: list[str] = []

    def check(name: str, ok: bool, detail: str = "") -> None:
        if ok:
            print(f"  OK  {name}")
        else:
            print(f" FAIL {name} {detail}")
            failures.append(name)

    print("=== End-to-end Flask screen checks ===\n")

    # CSS is compiled into each page as an inline <style> block
    r = client.get("/products")
    check("products page carries a <style> block", b"<style>" in r.data)
    check("style block survived compilation", b"font-family" in r.data)

    # Home redirect → products
    r = client.get("/", follow_redirects=True)
    check("GET / redirects to products", r.status_code == 200 and b"Laptop" in r.data)

    # List products
    r = client.get("/products")
    check("GET /products shows Laptop", r.status_code == 200 and b"Laptop" in r.data)
    check("GET /products shows Headphones", b"Headphones" in r.data)
    check("GET /products has Add link", b"/products/add" in r.data)
    check("GET /products has View link", b"/products/details?id=1" in r.data)
    check("GET /products has Delete link", b"/products/delete?id=" in r.data)
    check("GET /products has no external stylesheet", b'rel="stylesheet"' not in r.data)

    # Details
    r = client.get("/products/details?id=1")
    check("GET /products/details?id=1", r.status_code == 200 and b"Laptop" in r.data and b"Available" in r.data)

    r = client.get("/products/details?id=2")
    check("GET /products/details?id=2", r.status_code == 200 and b"Headphones" in r.data)

    # Add page
    r = client.get("/products/add")
    check("GET /products/add", r.status_code == 200 and b"Save Product" in r.data)
    check("GET /products/add has back link", b"/products" in r.data)

    # Create product
    r = client.post("/products/create", data={"name": "Tablet", "price": "400"}, follow_redirects=True)
    check("POST /products/create adds Tablet", r.status_code == 200 and b"Tablet" in r.data)

    # Delete product (id=2 Headphones)
    r = client.get("/products/delete?id=2", follow_redirects=True)
    check("GET /products/delete?id=2", r.status_code == 200 and b"Headphones" not in r.data)
    check("After delete Laptop remains", b"Laptop" in r.data)

    print()
    if failures:
        print(f"FAILED ({len(failures)}): {', '.join(failures)}")
        sys.exit(1)

    print("All Flask navigation checks passed.")
    print("To try in a browser:  cd src/Tests/FinalTests && python app.py")
    print("Then open http://127.0.0.1:5001/products")


if __name__ == "__main__":
    main()
