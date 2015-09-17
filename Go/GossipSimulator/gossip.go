package main

import (
"fmt"
"strconv"
"math/rand"
"time"
)

type Message struct {
    msgType int
    content string
}

type Peer struct {
    id int
    neighboor []Peer
    count int
    msgQueue chan Message
}

func (p Peer) Start(converge chan bool) {
    for {
        select {
        case msg := <-p.msgQueue:
            p.count++
            p.ProcessMessage(msg)
            if p.count < 4 {
                p.SendMessage(Message{0, "Message from Peer " + strconv.Itoa(p.id)})
            }
            if p.count == 3 {
                converge <- true
                fmt.Println("Peer " + strconv.Itoa(p.id) + " converge")
            }
        default:
            time.Sleep(1 * time.Second)
        }
    }
}

func (p Peer) SendMessage(msg Message) {
    i := rand.Intn(len(p.neighboor))
    p.neighboor[i].msgQueue <- msg
}

func (p Peer) ProcessMessage(msg Message) {
    fmt.Println("Peer " + strconv.Itoa(p.id) + " receive " + msg.content + " [count=" + strconv.Itoa(p.count) + "]")
}

func main() {

    const n = 3
    // create peers
    var peers [n]Peer
    for i :=0; i < n; i++ {
        peers[i] = Peer{id:i, count:0, msgQueue:make(chan Message)}
    }
    mainp := Peer{id:-1, count:0, msgQueue:make(chan Message)}
    // build topology
    for i :=0; i < n; i++ {
        for j :=0; j < n; j++ {
            if j != i {
                peers[i].neighboor = append(peers[i].neighboor, peers[j])
            }
        }
        mainp.neighboor = append(mainp.neighboor, peers[i])
    }

    converge := make(chan bool)
    go mainp.SendMessage(Message{0, "Hello, I am p0"})
    for i :=0; i < n; i++ {
        go peers[i].Start(converge)
    }

    for i := 0; i < n; i++ {
        <-converge
    }
}
