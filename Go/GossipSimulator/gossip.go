package main

import (
"fmt"
"math/rand"
"time"
)

const numOfPeers = 5 // greater than 1
const threshold = 10

type Message struct {
    msgType int
    content string
    sender *Peer
}

type Peer struct {
    id int
    neighboor []Peer
    count int
    msgQueue chan Message
}

func (p Peer) Start(convergent chan bool) {
    msg := <-p.msgQueue
    fmt.Printf("[KNOWS] Peer %d knows the roumor\n", p.id)
    p.count++
    p.ProcessMessage(msg)
    p.SendMessage(msg.content)
    p.Propagate(convergent, msg)
}

func (p Peer) Propagate(convergent chan bool, msg Message) {
    for {
        select {
        case m := <-p.msgQueue:
            if m.msgType == 0 {
                p.count++
                p.ProcessMessage(m)
                if p.count < threshold {
                    p.SendMessage(m.content)
                }
                if p.count >= threshold {
                    if p.count == threshold {
                        fmt.Printf("[CONVERGE] Peer %d converged\n", p.id)
                        convergent <- true
                    }
                    p.NotifyTerminate(m)
                }
            } else {
                for i, n := range p.neighboor {
                    if m.sender.id == n.id {
                        p.neighboor = append(p.neighboor[:i], p.neighboor[i+1:]...)
                        break;
                    }
                }
                if len(p.neighboor) == 0 {
                    p.count = threshold + 1
                    fmt.Printf("[CONVERGE] Peer %d converged\n", p.id)
                    convergent <- true
                } else {
                    p.SendMessage(m.content)
                }
            }
        default:
            time.Sleep(100 * time.Millisecond)
        }

    }
}

func (p Peer) SendMessage(content string) {
    i := rand.Intn(len(p.neighboor))
    p.neighboor[i].msgQueue <- Message{0, content, &p}
}

func (p Peer) SendTick() {
    p.msgQueue <- Message{1, "Tick", &p}
}

func (p Peer) ProcessMessage(msg Message) {
    fmt.Printf("Peer %d receive %s from Peer %d\n" , p.id, msg.content, msg.sender.id)
}

func (p Peer) NotifyTerminate(msg Message) {
    msg.sender.msgQueue <- Message{2, msg.content, &p}
    fmt.Printf("Peer %d notify terminate to %d\n", p.id, msg.sender.id)
}

func BuildFUllNetwork(peers []Peer) {
    for i :=1; i < numOfPeers + 1; i++ {
        for j :=1; j < numOfPeers + 1; j++ {
            if j != i {
                peers[i].neighboor = append(peers[i].neighboor, peers[j])
            }
        }
    }
}

func main() {
    // create peers
    var peers [numOfPeers + 1]Peer
    for i := 0; i < numOfPeers + 1; i++ {
        peers[i] = Peer{id:i, count:0, msgQueue:make(chan Message)}
    }
    // build topology
    BuildFUllNetwork(peers[:])
    for i := 1; i < numOfPeers + 1; i++ {
        peers[0].neighboor = append(peers[0].neighboor, peers[i])
    }
    convergent := make(chan bool)
    rand.Seed( time.Now().UTC().UnixNano())
    for i := 1; i < numOfPeers + 1; i++ {
        go peers[i].Start(convergent)
    }
    peers[0].SendMessage("Hello, I am main process")
    for i := 0; i < numOfPeers; i++ {
        <-convergent
    }
}
