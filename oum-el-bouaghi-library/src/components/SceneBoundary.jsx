import { Component } from 'react'

/** إن فشل WebGL لأي سبب نعود إلى البديل الثابت بصمت */
export default class SceneBoundary extends Component {
  state = { failed: false }
  static getDerivedStateFromError() {
    return { failed: true }
  }
  componentDidCatch() {
    this.props.onFail?.()
  }
  render() {
    return this.state.failed ? null : this.props.children
  }
}
