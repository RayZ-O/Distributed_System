package main

import (
"fmt"
"log";
"net"
"encoding/gob"
"strings"
"time"
"os"
"strconv"
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
        fmt.Println("Usage: go run server.go [number of 0s(1-32)]")
        return
    }
    num0s, err := strconv.Atoi(os.Args[1])
    if err != nil  { //|| num0s < 1 || num0s > 32
        fmt.Println("Usage: go run server.go [number of 0s(1-32)]")
        return
    }
    // start server
    ln, err := net.Listen("tcp", ":5150")
    if err != nil {
        return
    }
    fmt.Println("Server is listening on 5150...")
    // produce work and assign to clients
    workchan := make(chan Work)
    go produceWork(1000, 6300000, num0s, "ruizhang;", workchan)
    for clientId := 0; ; clientId++{
        conn, err := ln.Accept()
        if err != nil {
            log.Println(err)
            continue
        }
        go sendWork(conn, workchan, clientId)
        go getResult(conn, clientId)
    }
}

func produceWork(unit int, size int, num0s int, base string, workchan chan Work) {
    prefix := strings.Repeat("0", num0s)
    cur := 0
    for size > unit {
        workchan <- Work{cur, cur + unit, base, prefix}
        cur += unit
        size -= unit
    }
    if size > 0 {
        workchan <- Work{cur, cur + size, base, prefix}
    }
    // close workchan when all works are assigned
    close(workchan)
}

func sendWork(c net.Conn, workchan chan Work, clientId int) {
    for {
        // assign works until workchan close
        select {
        case work, ok := <-workchan:
            if ok {
                fmt.Printf("Assigned work to [%d-%d] to client %d\n", work.StartVal, work.EndVal, clientId)
                encoder := gob.NewEncoder(c)
                err := encoder.Encode(work) 
                if err != nil {
                    fmt.Printf("Client %d disconnected, lost Job [%d-%d]\n", clientId, work.StartVal, work.EndVal)
                    return
                }
            } else {
                // if workchan is closed, send complete message to worker
                encoder := gob.NewEncoder(c)
                err := encoder.Encode(Work{-1, 0, "", ""}) 
                if err != nil {
                    fmt.Printf("Send complete message to client %d failed\n", clientId)
                }
                return
            }
        default:
            time.Sleep(1 * time.Second)
        }
    }   
}

func getResult(c net.Conn, clientId int) {
    for {
        decoder := gob.NewDecoder(c)
        res := &Result{}
        err := decoder.Decode(res)
        if err != nil {
            fmt.Printf("Client %d disconnected\n", clientId)
            return
        }
        fmt.Println("[Coin]" + res.Str + " " + res.Hash)        
    }
}
