package main 

import (
// "fmt"
"crypto/sha256"
"encoding/hex"
"strings"
)

func generate(charset string, c chan string, combo string, length int) {
    if length == 0 {
        c <- combo
        return
    }
    for _, ch := range charset {
        newcombo := combo + string(ch)        
        generate(charset, c, newcombo, length - 1)
    }
}

func produce(charset string, c chan string) {
    for i := 0; ; i++ {
        generate(charset, c, "", i)
    }
}

func sha256Value(text string) string {
    hasher := sha256.New()
    hasher.Write([]byte(text))
    return hex.EncodeToString(hasher.Sum(nil))
    
}

func checkPrefix(text string, prefix string) bool {
    if strings.HasPrefix(sha256Value(text), prefix) {
        return true
    } 
    return false
}

func consume(c chan string, text string, prefix string, found chan string) {
    for { 
        newtext := text + <-c
        if checkPrefix(newtext, prefix) {
            found <- newtext
        }
    }
}

func main() {  
    const charsetnum = "0123456789"
    const charsetalph = "abcdefghijklmnopqrstuvwxyz"

    // k := 6
    // text := "ruizhang;"
    for { }
    // 2 char sets, 2 channels
    // num_consumers |    1    |    5    |   10    |    20   |    40   |   100   |  
    // ---------------------------------------------------------------------------
    //    time(s)    | 12.298s | 10.234s | 10.179s | 10.043s | 10.116s | 10.274s |
    // c1 := make(chan string) 
    // c2 := make(chan string) 
    // go produce(charsetnum, c1)
    // go produce(charsetalph, c2)
    // prefix := strings.Repeat("0", k)
    // found := make(chan string)    
    // num_consumers := 5
    // for i := 0; i < num_consumers; i++ {
    //     go consume(c1, text, prefix, found)
    //     go consume(c2, text, prefix, found)
    // }

    // result := <-found
    // fmt.Println(result, sha256Value(result))
}

// Other versions

// 1 char sets(num)
// num_consumers |    1    |    5    |   10    |    20   |    40   |   100   |  
// ---------------------------------------------------------------------------
//    time(s)    | 10.834s |  8.887s | 6.725s  |  6.338s |  6.242s |  6.321s |
// output: ruizhang;4099087 
// 000000350de14b4d572542919af9e6129bef947b443d8f56b042308f34a862ef
/*
c := make(chan string) 
go produce(charsetnum, c)
prefix := strings.Repeat("0", k)
found := make(chan string)    
num_consumers := 100
for i := 0; i < num_consumers; i++ {
    go consume(c, text, prefix, found)
}
*/

// 1 char sets(alphabet)
// num_consumers |    1    |    5    |   10    |    20   |    40   |   100   |  
// ---------------------------------------------------------------------------
//    time(s)    |1m21.046s|1m2.590s | 55.697s | 45.831s | 50.245s | 57.997s |
// output: ruizhang;cdmxfv 
// 0000003a8204e34f56fa8c923a255c1fd3af61d9957705490a341216bdae17e9
/*
c := make(chan string) 
go produce(charsetalph, c)
prefix := strings.Repeat("0", k)
found := make(chan string)
num_consumers := 100
for i := 0; i < num_consumers; i++ {
    go consume(c, text, prefix, found)
}
*/

// 2 char sets, 1 channels
// num_consumers |    1    |    5    |   10    |    20   |    40   |   100   |  
// ---------------------------------------------------------------------------
//    time(s)    | 17.867s | 11.946s | 11.992s | 11.908s | 12.152s | 12.230s |
/*
c := make(chan string) 
go produce(charsetnum, c)
go produce(charsetalph, c)
prefix := strings.Repeat("0", k)
found := make(chan string)    
num_consumers := 100
for i := 0; i < num_consumers; i++ {
    go consume(c, text, prefix, found)
}
*/