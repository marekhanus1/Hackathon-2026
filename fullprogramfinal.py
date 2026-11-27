from math import *
import time

def app(value: int) -> None:
    start = time.time()
    while True:
        if value > 5000000000:
            end = time.time()
            print(end - start)
            print("AAAAAAAAAAAAAAAAAAAAAAA MAKEJ VOLE")
            break
        value += 1
    
    
    
def main() -> None:
    app(0)


if __name__ == '__main__':
    main()