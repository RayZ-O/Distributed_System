package main

import (
"fmt"
"strconv"
"math/rand"
"time"
)

const numOfPeers = 5  // greater than 1
const threshold = 10

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

func (p Peer) Start(convergent chan bool) {
    msg := <-p.msgQueue
    p.count++
    // fmt.Println("[NEW] Peer " + strconv.Itoa(p.id) + " knows the roumor")
    go func (msg Message) {
        for {
            if p.count <= threshold {
                // fmt.Println(p.count)
                p.SendMessage(msg)
            }
            time.Sleep(100 * time.Millisecond)
        }

    } (msg)
    for {
        select {
        case <-p.msgQueue:
            p.count++
            p.ProcessMessage(msg)
            if p.count == threshold {
                fmt.Println("[CONVERGE] Peer " + strconv.Itoa(p.id) + " converged")
                convergent <- true
            }
        default:
            time.Sleep(10 * time.Millisecond)
        }

    }
}

func (p Peer) SendMessage(msg Message) {
    i := rand.Intn(len(p.neighboor))
    p.neighboor[i].msgQueue <- msg
}

func (p Peer) ProcessMessage(msg Message) {
    // fmt.Println("Peer " + strconv.Itoa(p.id) + " receive " + msg.content + " [count=" + strconv.Itoa(p.count) + "]")
}

func BuildFUllNetwork(peers []Peer) {
    for i :=0; i < numOfPeers; i++ {
        for j :=0; j < numOfPeers; j++ {
            if j != i {
                peers[i].neighboor = append(peers[i].neighboor, peers[j])
            }
        }
    }
}

func BuildLine(peers []Peer) {
    peers[0].neighboor = append(peers[0].neighboor, peers[1])
    for i :=1; i < numOfPeers - 1; i++ {
        peers[i].neighboor = append(peers[i].neighboor, peers[i - 1])
        peers[i].neighboor = append(peers[i].neighboor, peers[i + 1])
    }
    peers[numOfPeers - 1].neighboor = append(peers[numOfPeers - 1].neighboor, peers[numOfPeers - 2])
}

func main() {
    // create peers
    var peers [numOfPeers]Peer
    for i :=0; i < numOfPeers; i++ {
        peers[i] = Peer{id:i, count:0, msgQueue:make(chan Message)}
    }
    mainp := Peer{id:-1, count:0, msgQueue:make(chan Message)}
    // build topology
    BuildFUllNetwork(peers[:])
    // BuildLine(peers[:])
    for i :=0; i < numOfPeers; i++ {
        mainp.neighboor = append(mainp.neighboor, peers[i])
    }

    convergent := make(chan bool)
    rand.Seed( time.Now().UTC().UnixNano())
    go mainp.SendMessage(Message{0, "Hello, I am main process"})
    for i :=0; i < numOfPeers; i++ {
        go peers[i].Start(convergent)
    }
    for i :=0; i < numOfPeers; i++ {
        <-convergent
    }
}
