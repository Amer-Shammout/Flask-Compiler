x = 10                # DEFINITION: x in global scope

def outer():
    y = 20            # DEFINITION: y in function:outer
    x = 30            # DEFINITION: x in function:outer (shadows global x) → SHADOWED definition
    print(x)          # REFERENCE: x → RESOLVED (local x, but shadows outer) → SHADOWED reference
    print(y)          # REFERENCE: y → RESOLVED (local y)
    print(z)          # REFERENCE: z → UNDEFINED (no definition in any accessible scope)
    print(len([1]))   # REFERENCE: len → RESOLVED (Python builtins)

def another():
    print(y)          # REFERENCE: y → UNDEFINED (y is in outer's scope, not accessible here)
    print(x)          # REFERENCE: x → RESOLVED (global x, since outer's x is not in this chain)