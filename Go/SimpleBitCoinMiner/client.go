package main

import (
"fmt"
"net"
"encoding/gob"
"strconv"
"crypto/sha256"
"encoding/hex"
"strings"
"os"
)

type Work struct {
    StartVal int
    EndVal int
    BaseStr string
    Prefix string
}

type Result struct {
    Str string
    Hash string
}

func main() {
    // check input arguments, first arg is the path to the program
    if len(os.Args) != 2 {
        fmt.Println("Usage: go run client.go [server IP]")
        return
    }    
    conn, err := net.Dial("tcp", os.Args[1] + ":5150")
    if err != nil {
        fmt.Println("[ERROR]Can not connect to server")
        return
    }
    fmt.Println("[INFO]Start client");

    reschan := make(chan Result)
    endchan := make(chan bool)
    go doWork(conn, reschan, endchan)
    go sendResult(conn, reschan, endchan)

    <-endchan
    conn.Close()
}

// return SHA-256 value of given string
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

// mine bitcoin and put them into reschan
func mine(start int, end int, base string, prefix string, conn net.Conn, reschan chan Result) {
    for i := start; i < end; i++ {
        text := base + strconv.Itoa(i)
        if checkPrefix(text, prefix) {
            reschan <- Result{text, sha256Value(text)}
        }
    } 
}

// get works from server and mine
func doWork(conn net.Conn, reschan chan Result, endchan chan bool) {
    for i := 0; ; i++ {
        decoder := gob.NewDecoder(conn)
        work := &Work{}
        err := decoder.Decode(work)
        if err != nil {
            fmt.Println("[ERROR]Failed to get work ")         
            endchan <- true
        }
        // only the complete message has work.StartVal = -1
        if work.StartVal == -1 {
            fmt.Println("[INFO]Work complete")
            endchan <- true
            return
        }
        mine(work.StartVal, work.EndVal, work.BaseStr, work.Prefix, conn, reschan)
        fmt.Printf("[INFO]Task %d finished\n", i);
    }    
}

// send result to server
func sendResult(conn net.Conn, reschan chan Result, endchan chan bool) {
    for {
        encoder := gob.NewEncoder(conn)
        err := encoder.Encode(<-reschan) 
        if err != nil {
            fmt.Println("[ERROR]Failed to send result")
            endchan <- true
        }
    }    
}
